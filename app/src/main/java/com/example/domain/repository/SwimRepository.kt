package com.example.domain.repository

import com.example.data.local.dao.SwimTrackDao
import com.example.data.local.entities.CompetitionEntity
import com.example.data.local.entities.CompetitionResultEntity
import com.example.data.local.entities.SwimRecordEntity
import com.example.domain.model.Competition
import com.example.domain.model.CompetitionResult
import com.example.domain.model.CompetitionWithResults
import com.example.domain.model.NewPbCelebration
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.SessionType
import com.example.domain.model.StartType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.domain.usecase.SwimUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class SaveRecordResult(
    val recordId: Long,
    val newPbCelebration: NewPbCelebration?
)

class SwimRepository(
    private val dao: SwimTrackDao
) {

    val allRecordsFlow: Flow<List<SwimRecord>> = combine(
        dao.observeAllSwimRecords(),
        dao.observeAllCompetitions()
    ) { recordEntities, competitionEntities ->
        val competitionNameMap = competitionEntities.associate { it.id to it.name }
        val rawRecords = recordEntities.map { entity ->
            entity.toDomain(competitionName = entity.competitionId?.let { competitionNameMap[it] })
        }
        SwimUseCases.annotatePersonalBests(rawRecords)
    }

    val allCompetitionsWithResultsFlow: Flow<List<CompetitionWithResults>> = combine(
        dao.observeAllCompetitions(),
        dao.observeAllCompetitionResults(),
        allRecordsFlow
    ) { competitions, results, annotatedRecords ->
        val pbRecordIds = annotatedRecords.filter { it.isPersonalBest }.map { it.id }.toSet()
        val resultsByComp = results.groupBy { it.competitionId }
        val recordsByComp = annotatedRecords
            .filter { it.competitionId != null }
            .groupBy { it.competitionId!! }

        competitions.map { compEntity ->
            val explicitResults = (resultsByComp[compEntity.id] ?: emptyList()).map { resEntity ->
                resEntity.toDomain(
                    isPersonalBest = resEntity.swimRecordId != null && pbRecordIds.contains(resEntity.swimRecordId)
                )
            }
            val linkedRecordIds = explicitResults.mapNotNull { it.swimRecordId }.toSet()
            val unlinkedRecordsAsResults = (recordsByComp[compEntity.id] ?: emptyList())
                .filter { it.id !in linkedRecordIds }
                .map { rec ->
                    CompetitionResult(
                        id = -rec.id,
                        competitionId = compEntity.id,
                        swimRecordId = rec.id,
                        distance = rec.distance,
                        stroke = rec.stroke,
                        poolLength = rec.poolLength,
                        startType = rec.startType,
                        resultTimeMillis = rec.timeMillis,
                        status = rec.status,
                        notes = rec.notes,
                        isPersonalBest = rec.isPersonalBest
                    )
                }

            CompetitionWithResults(
                competition = compEntity.toDomain(),
                results = explicitResults + unlinkedRecordsAsResults
            )
        }
    }

    suspend fun getRecordById(id: Long): SwimRecord? = withContext(Dispatchers.IO) {
        val entity = dao.getSwimRecordById(id) ?: return@withContext null
        val compName = entity.competitionId?.let { dao.getCompetitionById(it)?.name }
        entity.toDomain(competitionName = compName)
    }

    /**
     * Saves a new or edited [SwimRecord] and detects if it sets a new Personal Best (PB)
     * for its (Distance + Stroke).
     */
    suspend fun saveSwimRecord(
        record: SwimRecord,
        competitionOnlyPb: Boolean = false
    ): SaveRecordResult = withContext(Dispatchers.IO) {
        val existingRecords = dao.getAllSwimRecordsOnce().filter { it.id != record.id }
        val eligiblePrevious = existingRecords.filter {
            it.distance == record.distance &&
                it.stroke == record.stroke &&
                it.status == ResultStatus.FINISHED &&
                it.time > 0L &&
                (!competitionOnlyPb || it.sessionType == SessionType.COMPETITION)
        }
        val previousBestMillis = eligiblePrevious.minOfOrNull { it.time }

        val entity = SwimRecordEntity.fromDomain(record)
        val savedId = if (record.id == 0L) {
            dao.insertSwimRecord(entity)
        } else {
            dao.updateSwimRecord(entity)
            record.id
        }

        // Sync linked competition_result when a record is linked to a competition
        val linkedCompResult = dao.getCompetitionResultBySwimRecordId(savedId)
        val targetCompId = if (record.sessionType == SessionType.COMPETITION) record.competitionId else null
        if (targetCompId != null) {
            if (linkedCompResult != null) {
                dao.updateCompetitionResult(
                    linkedCompResult.copy(
                        competitionId = targetCompId,
                        distance = record.distance,
                        stroke = record.stroke,
                        poolLength = record.poolLength,
                        startType = record.startType,
                        resultTime = record.timeMillis,
                        status = record.status,
                        notes = record.notes
                    )
                )
            } else {
                dao.insertCompetitionResult(
                    CompetitionResultEntity(
                        competitionId = targetCompId,
                        swimRecordId = savedId,
                        distance = record.distance,
                        stroke = record.stroke,
                        poolLength = record.poolLength,
                        startType = record.startType,
                        resultTime = record.timeMillis,
                        status = record.status,
                        notes = record.notes
                    )
                )
            }
        } else if (linkedCompResult != null) {
            dao.deleteCompetitionResultBySwimRecordId(savedId)
        }

        val isEligibleForPb = record.status == ResultStatus.FINISHED &&
            record.timeMillis > 0L &&
            (!competitionOnlyPb || record.sessionType == SessionType.COMPETITION)

        val isNewPb = isEligibleForPb &&
            (previousBestMillis == null || record.timeMillis < previousBestMillis)

        val celebration = if (isNewPb) {
            NewPbCelebration(
                eventTitle = record.eventTitle,
                distance = record.distance,
                stroke = record.stroke,
                poolLength = record.poolLength,
                newPbMillis = record.timeMillis,
                previousPbMillis = previousBestMillis,
                improvementMillis = previousBestMillis?.let { it - record.timeMillis }
            )
        } else {
            null
        }

        SaveRecordResult(recordId = savedId, newPbCelebration = celebration)
    }

    suspend fun deleteSwimRecord(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteCompetitionResultBySwimRecordId(id)
        dao.deleteSwimRecordById(id)
    }

    // --- Competitions ---
    suspend fun saveCompetition(competition: Competition): Long = withContext(Dispatchers.IO) {
        val entity = CompetitionEntity.fromDomain(competition)
        if (competition.id == 0L) {
            dao.insertCompetition(entity)
        } else {
            dao.updateCompetition(entity)
            competition.id
        }
    }

    suspend fun deleteCompetition(competitionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteResultsForCompetition(competitionId)
        dao.deleteSwimRecordsForCompetition(competitionId)
        dao.deleteCompetitionById(competitionId)
    }

    /**
     * Saves a [CompetitionResult] inside a competition and syncs it with `swim_records`.
     * Note: DQ, DNS, and NS results are saved with their non-finished status so they NEVER count as PBs.
     */
    suspend fun saveCompetitionResult(
        result: CompetitionResult,
        competitionDate: Long,
        competitionOnlyPb: Boolean = false
    ): NewPbCelebration? = withContext(Dispatchers.IO) {
        val existingRecords = dao.getAllSwimRecordsOnce().filter { it.id != (result.swimRecordId ?: -1L) }
        val eligiblePrevious = existingRecords.filter {
            it.distance == result.distance &&
                it.stroke == result.stroke &&
                it.status == ResultStatus.FINISHED &&
                it.time > 0L &&
                (!competitionOnlyPb || it.sessionType == SessionType.COMPETITION)
        }
        val previousBestMillis = eligiblePrevious.minOfOrNull { it.time }

        val effectiveTime = if (result.status == ResultStatus.FINISHED) {
            result.resultTimeMillis ?: 0L
        } else {
            result.resultTimeMillis ?: 0L
        }

        // Create or update corresponding SwimRecordEntity so history & stats stay unified
        val swimRecordEntity = SwimRecordEntity(
            id = result.swimRecordId ?: 0L,
            date = competitionDate,
            time = effectiveTime,
            distance = result.distance,
            stroke = result.stroke,
            sessionType = SessionType.COMPETITION,
            poolLength = result.poolLength,
            startType = result.startType,
            competitionId = result.competitionId,
            status = result.status,
            notes = result.notes
        )

        val recordId = if (swimRecordEntity.id == 0L) {
            dao.insertSwimRecord(swimRecordEntity)
        } else {
            dao.updateSwimRecord(swimRecordEntity)
            swimRecordEntity.id
        }

        val existingLinked = result.swimRecordId?.let { dao.getCompetitionResultBySwimRecordId(it) }
        val actualResultId = if (result.id > 0L) result.id else (existingLinked?.id ?: 0L)

        val resultEntity = CompetitionResultEntity.fromDomain(
            result.copy(id = actualResultId, swimRecordId = recordId)
        )
        if (resultEntity.id == 0L) {
            dao.insertCompetitionResult(resultEntity)
        } else {
            dao.updateCompetitionResult(resultEntity)
        }

        val isNewPb = result.status == ResultStatus.FINISHED &&
            effectiveTime > 0L &&
            (previousBestMillis == null || effectiveTime < previousBestMillis)

        if (isNewPb) {
            NewPbCelebration(
                eventTitle = result.eventTitle,
                distance = result.distance,
                stroke = result.stroke,
                poolLength = result.poolLength,
                newPbMillis = effectiveTime,
                previousPbMillis = previousBestMillis,
                improvementMillis = previousBestMillis?.let { it - effectiveTime }
            )
        } else {
            null
        }
    }

    suspend fun deleteCompetitionResult(result: CompetitionResult) = withContext(Dispatchers.IO) {
        if (result.id > 0L) {
            dao.deleteCompetitionResultById(result.id)
        }
        result.swimRecordId?.let {
            dao.deleteCompetitionResultBySwimRecordId(it)
            dao.deleteSwimRecordById(it)
        }
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        dao.clearEntireDatabase()
    }

    // --- Export / Import JSON ---
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val competitions = dao.getAllCompetitionsOnce()
        val records = dao.getAllSwimRecordsOnce()
        val results = dao.getAllCompetitionResultsOnce()

        val root = JSONObject()
        root.put("app", "SwimTrack")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val compArray = JSONArray()
        competitions.forEach { c ->
            compArray.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("date", c.date)
                    put("location", c.location)
                    put("notes", c.notes)
                }
            )
        }
        root.put("competitions", compArray)

        val recArray = JSONArray()
        records.forEach { r ->
            recArray.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    put("time", r.time)
                    put("distance", r.distance.name)
                    put("stroke", r.stroke.name)
                    put("sessionType", r.sessionType.name)
                    put("poolLength", r.poolLength.name)
                    put("startType", r.startType.name)
                    if (r.competitionId != null) put("competitionId", r.competitionId)
                    put("status", r.status.name)
                    put("notes", r.notes)
                    put("createdAt", r.createdAt)
                }
            )
        }
        root.put("swimRecords", recArray)

        val resArray = JSONArray()
        results.forEach { cr ->
            resArray.put(
                JSONObject().apply {
                    put("id", cr.id)
                    put("competitionId", cr.competitionId)
                    if (cr.swimRecordId != null) put("swimRecordId", cr.swimRecordId)
                    put("distance", cr.distance.name)
                    put("stroke", cr.stroke.name)
                    put("poolLength", cr.poolLength.name)
                    put("startType", cr.startType.name)
                    if (cr.heat != null) put("heat", cr.heat)
                    if (cr.lane != null) put("lane", cr.lane)
                    if (cr.place != null) put("place", cr.place)
                    if (cr.seedTime != null) put("seedTime", cr.seedTime)
                    if (cr.resultTime != null) put("resultTime", cr.resultTime)
                    put("status", cr.status.name)
                    put("notes", cr.notes)
                }
            )
        }
        root.put("competitionResults", resArray)

        root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("JSON file is empty."))
            }
            val root = JSONObject(trimmed)
            if (!root.has("swimRecords") && !root.has("competitions")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid SwimTrack backup JSON: missing 'swimRecords' or 'competitions'.")
                )
            }

            val compArray = root.optJSONArray("competitions") ?: JSONArray()
            val recArray = root.optJSONArray("swimRecords") ?: JSONArray()
            val resArray = root.optJSONArray("competitionResults") ?: JSONArray()

            val competitionsToInsert = mutableListOf<CompetitionEntity>()
            for (i in 0 until compArray.length()) {
                val obj = compArray.getJSONObject(i)
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                competitionsToInsert.add(
                    CompetitionEntity(
                        id = obj.optLong("id", 0L),
                        name = name,
                        date = obj.optLong("date", System.currentTimeMillis()),
                        location = obj.optString("location", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }

            val recordsToInsert = mutableListOf<SwimRecordEntity>()
            for (i in 0 until recArray.length()) {
                val obj = recArray.getJSONObject(i)
                val time = obj.optLong("time", -1L)
                val status = ResultStatus.fromString(obj.optString("status", ResultStatus.FINISHED.name))
                if (status == ResultStatus.FINISHED && time <= 0L) continue
                recordsToInsert.add(
                    SwimRecordEntity(
                        id = obj.optLong("id", 0L),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        time = time.coerceAtLeast(0L),
                        distance = SwimDistance.fromString(obj.optString("distance", SwimDistance.D50M.name)),
                        stroke = Stroke.fromString(obj.optString("stroke", Stroke.FREESTYLE.name)),
                        sessionType = SessionType.fromString(obj.optString("sessionType", SessionType.TRAINING.name)),
                        poolLength = PoolLength.fromString(obj.optString("poolLength", PoolLength.POOL_25M.name)),
                        startType = StartType.fromString(obj.optString("startType", StartType.DIVE.name)),
                        competitionId = if (obj.has("competitionId") && !obj.isNull("competitionId")) obj.optLong("competitionId") else null,
                        status = status,
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val resultsToInsert = mutableListOf<CompetitionResultEntity>()
            for (i in 0 until resArray.length()) {
                val obj = resArray.getJSONObject(i)
                val compId = obj.optLong("competitionId", 0L)
                if (compId == 0L) continue
                resultsToInsert.add(
                    CompetitionResultEntity(
                        id = obj.optLong("id", 0L),
                        competitionId = compId,
                        swimRecordId = if (obj.has("swimRecordId") && !obj.isNull("swimRecordId")) obj.optLong("swimRecordId") else null,
                        distance = SwimDistance.fromString(obj.optString("distance", SwimDistance.D50M.name)),
                        stroke = Stroke.fromString(obj.optString("stroke", Stroke.FREESTYLE.name)),
                        poolLength = PoolLength.fromString(obj.optString("poolLength", PoolLength.POOL_50M.name)),
                        startType = StartType.fromString(obj.optString("startType", StartType.DIVE.name)),
                        heat = if (obj.has("heat") && !obj.isNull("heat")) obj.optInt("heat") else null,
                        lane = if (obj.has("lane") && !obj.isNull("lane")) obj.optInt("lane") else null,
                        place = if (obj.has("place") && !obj.isNull("place")) obj.optInt("place") else null,
                        seedTime = if (obj.has("seedTime") && !obj.isNull("seedTime")) obj.optLong("seedTime") else null,
                        resultTime = if (obj.has("resultTime") && !obj.isNull("resultTime")) obj.optLong("resultTime") else null,
                        status = ResultStatus.fromString(obj.optString("status", ResultStatus.FINISHED.name)),
                        notes = obj.optString("notes", "")
                    )
                )
            }

            dao.insertCompetitions(competitionsToInsert)
            dao.insertSwimRecords(recordsToInsert)
            dao.insertCompetitionResults(resultsToInsert)

            Result.success(recordsToInsert.size + competitionsToInsert.size)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse JSON backup: ${e.localizedMessage ?: "Invalid format"}"))
        }
    }

    // --- Sample Demo Data (Optional, user-triggered, easily removable) ---
    suspend fun loadDemoSampleData() = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val dayMs = 24 * 60 * 60 * 1000L

        // Create sample Competition: Giza Summer Championship 2026
        val gizaCompDate = now - (18 * dayMs)
        val gizaCompId = dao.insertCompetition(
            CompetitionEntity(
                name = "Giza Summer Championship 2026",
                date = gizaCompDate,
                location = "Giza Olympic Aquatic Center (50m)",
                notes = "Regional summer championship meet"
            )
        )

        val regionalSprintDate = now - (5 * dayMs)
        val regionalCompId = dao.insertCompetition(
            CompetitionEntity(
                name = "Cairo Autumn Sprint Cup",
                date = regionalSprintDate,
                location = "Cairo International Pool (25m)",
                notes = "Short course sprint invitational"
            )
        )

        // 1. 50m Freestyle progression: 38.10, 37.04 (Giza Comp), 36.50, 35.90, 34.80, 33.20
        val free50Times = listOf(
            Triple("38.10", now - 60 * dayMs, SessionType.TRAINING),
            Triple("37.04", gizaCompDate, SessionType.COMPETITION),
            Triple("36.50", now - 35 * dayMs, SessionType.TRAINING),
            Triple("35.90", now - 24 * dayMs, SessionType.TIME_TRIAL),
            Triple("34.80", now - 12 * dayMs, SessionType.TRAINING),
            Triple("33.20", regionalSprintDate, SessionType.COMPETITION)
        )
        free50Times.forEachIndexed { idx, (timeStr, dateMs, session) ->
            val timeMs = SwimTimeUtils.parseSwimTime(timeStr) ?: 35000L
            val compId = when (idx) {
                1 -> gizaCompId
                5 -> regionalCompId
                else -> null
            }
            val pool = if (idx == 1) PoolLength.POOL_50M else PoolLength.POOL_25M
            val recId = dao.insertSwimRecord(
                SwimRecordEntity(
                    date = dateMs,
                    time = timeMs,
                    distance = SwimDistance.D50M,
                    stroke = Stroke.FREESTYLE,
                    sessionType = session,
                    poolLength = pool,
                    startType = if (session == SessionType.TRAINING) StartType.PUSH else StartType.DIVE,
                    competitionId = compId,
                    status = ResultStatus.FINISHED,
                    notes = if (idx == 5) "Strong underwater dolphin kick off the turn!" else "Demo progression swim",
                    createdAt = dateMs + idx * 1000L
                )
            )
            if (idx == 1) {
                dao.insertCompetitionResult(
                    CompetitionResultEntity(
                        competitionId = gizaCompId,
                        swimRecordId = recId,
                        distance = SwimDistance.D50M,
                        stroke = Stroke.FREESTYLE,
                        poolLength = PoolLength.POOL_50M,
                        startType = StartType.DIVE,
                        heat = 4,
                        lane = 3,
                        place = 18,
                        seedTime = SwimTimeUtils.parseSwimTime("37.50"),
                        resultTime = timeMs,
                        status = ResultStatus.FINISHED,
                        notes = "Dropped 0.46s from seed time"
                    )
                )
            } else if (idx == 5) {
                dao.insertCompetitionResult(
                    CompetitionResultEntity(
                        competitionId = regionalCompId,
                        swimRecordId = recId,
                        distance = SwimDistance.D50M,
                        stroke = Stroke.FREESTYLE,
                        poolLength = PoolLength.POOL_25M,
                        startType = StartType.DIVE,
                        heat = 6,
                        lane = 4,
                        place = 3,
                        seedTime = SwimTimeUtils.parseSwimTime("34.80"),
                        resultTime = timeMs,
                        status = ResultStatus.FINISHED,
                        notes = "New personal best and podium finish!"
                    )
                )
            }
        }

        // 2. 100m Freestyle progression: 1:30.00, 1:27.50, 1:25.40, 1:23.00 (Giza Comp)
        val free100Times = listOf(
            Triple("1:30.00", now - 55 * dayMs, SessionType.TRAINING),
            Triple("1:27.50", now - 40 * dayMs, SessionType.TRAINING),
            Triple("1:25.40", now - 28 * dayMs, SessionType.TIME_TRIAL),
            Triple("1:23.00", gizaCompDate, SessionType.COMPETITION)
        )
        free100Times.forEachIndexed { idx, (timeStr, dateMs, session) ->
            val timeMs = SwimTimeUtils.parseSwimTime(timeStr) ?: 85000L
            val isGiza = idx == 3
            val recId = dao.insertSwimRecord(
                SwimRecordEntity(
                    date = dateMs,
                    time = timeMs,
                    distance = SwimDistance.D100M,
                    stroke = Stroke.FREESTYLE,
                    sessionType = session,
                    poolLength = if (isGiza) PoolLength.POOL_50M else PoolLength.POOL_25M,
                    startType = StartType.DIVE,
                    competitionId = if (isGiza) gizaCompId else null,
                    status = ResultStatus.FINISHED,
                    notes = if (isGiza) "Even splits 40.8 / 42.2" else "Aerobic pace set",
                    createdAt = dateMs + idx * 1000L
                )
            )
            if (isGiza) {
                dao.insertCompetitionResult(
                    CompetitionResultEntity(
                        competitionId = gizaCompId,
                        swimRecordId = recId,
                        distance = SwimDistance.D100M,
                        stroke = Stroke.FREESTYLE,
                        poolLength = PoolLength.POOL_50M,
                        startType = StartType.DIVE,
                        heat = 2,
                        lane = 1,
                        place = 12,
                        seedTime = SwimTimeUtils.parseSwimTime("1:25.00"),
                        resultTime = timeMs,
                        status = ResultStatus.FINISHED,
                        notes = "2.00s faster than seed time"
                    )
                )
            }
        }

        // 3. 50m Backstroke progression: 45.00, 43.80, 42.80 (Giza Comp)
        val back50Times = listOf(
            Triple("45.00", now - 48 * dayMs, SessionType.TRAINING),
            Triple("43.80", now - 30 * dayMs, SessionType.TRAINING),
            Triple("42.80", gizaCompDate, SessionType.COMPETITION)
        )
        back50Times.forEachIndexed { idx, (timeStr, dateMs, session) ->
            val timeMs = SwimTimeUtils.parseSwimTime(timeStr) ?: 43000L
            val isGiza = idx == 2
            val recId = dao.insertSwimRecord(
                SwimRecordEntity(
                    date = dateMs,
                    time = timeMs,
                    distance = SwimDistance.D50M,
                    stroke = Stroke.BACKSTROKE,
                    sessionType = session,
                    poolLength = if (isGiza) PoolLength.POOL_50M else PoolLength.POOL_25M,
                    startType = StartType.PUSH,
                    competitionId = if (isGiza) gizaCompId else null,
                    status = ResultStatus.FINISHED,
                    notes = "Backstroke breakout work",
                    createdAt = dateMs + idx * 1000L
                )
            )
            if (isGiza) {
                dao.insertCompetitionResult(
                    CompetitionResultEntity(
                        competitionId = gizaCompId,
                        swimRecordId = recId,
                        distance = SwimDistance.D50M,
                        stroke = Stroke.BACKSTROKE,
                        poolLength = PoolLength.POOL_50M,
                        startType = StartType.PUSH,
                        heat = 5,
                        lane = 4,
                        place = 9,
                        seedTime = SwimTimeUtils.parseSwimTime("43.80"),
                        resultTime = timeMs,
                        status = ResultStatus.FINISHED,
                        notes = "Clean finish on the wall"
                    )
                )
            }
        }
    }
}
