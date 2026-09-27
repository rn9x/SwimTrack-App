package com.example.domain.usecase

import com.example.domain.model.ChartRangeFilter
import com.example.domain.model.EventKey
import com.example.domain.model.EventStatistics
import com.example.domain.model.PersonalBestSummary
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.SessionType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import kotlin.math.roundToLong

object SwimUseCases {

    /**
     * Marks which records in [records] are the current Personal Best (PB) for their
     * (Distance + Stroke) combination.
     *
     * Rules:
     * - Only records with [ResultStatus.FINISHED] and `timeMillis > 0` are eligible.
     * - DQ, DNS, and NS records NEVER become PBs.
     * - If [competitionOnlyPb] is true, only [SessionType.COMPETITION] records can be PBs.
     * - If [separateByPoolLength] is true, PB is determined per (Distance + Stroke + PoolLength);
     *   otherwise per (Distance + Stroke).
     */
    fun annotatePersonalBests(
        records: List<SwimRecord>,
        competitionOnlyPb: Boolean = false,
        separateByPoolLength: Boolean = false
    ): List<SwimRecord> {
        val eligibleRecords = records.filter { record ->
            record.status == ResultStatus.FINISHED &&
                record.timeMillis > 0L &&
                (!competitionOnlyPb || record.sessionType == SessionType.COMPETITION)
        }

        val bestTimeByGroup: Map<Any, Long> = if (separateByPoolLength) {
            eligibleRecords
                .groupBy { Triple(it.distance, it.stroke, it.poolLength) }
                .mapValues { (_, group) -> group.minOf { it.timeMillis } }
        } else {
            eligibleRecords
                .groupBy { EventKey(it.distance, it.stroke) }
                .mapValues { (_, group) -> group.minOf { it.timeMillis } }
        }

        return records.map { record ->
            val eligible = record.status == ResultStatus.FINISHED &&
                record.timeMillis > 0L &&
                (!competitionOnlyPb || record.sessionType == SessionType.COMPETITION)
            val key: Any = if (separateByPoolLength) {
                Triple(record.distance, record.stroke, record.poolLength)
            } else {
                EventKey(record.distance, record.stroke)
            }
            val bestMillis = bestTimeByGroup[key]
            record.copy(isPersonalBest = eligible && bestMillis != null && record.timeMillis == bestMillis)
        }
    }

    /**
     * Computes PersonalBestSummary list across all events that have at least one valid finished time.
     */
    fun computePersonalBests(
        records: List<SwimRecord>,
        poolFilter: PoolLength? = null,
        sessionFilter: SessionType? = null,
        competitionOnlyPb: Boolean = false
    ): List<PersonalBestSummary> {
        val filtered = records.filter { record ->
            record.status == ResultStatus.FINISHED &&
                record.timeMillis > 0L &&
                (poolFilter == null || record.poolLength == poolFilter) &&
                (sessionFilter == null || record.sessionType == sessionFilter) &&
                (!competitionOnlyPb || record.sessionType == SessionType.COMPETITION)
        }

        return filtered
            .groupBy { EventKey(it.distance, it.stroke) }
            .map { (eventKey, eventRecords) ->
                // Sort by fastest time first; tie-break by most recent date
                val sortedByTime = eventRecords.sortedWith(
                    compareBy<SwimRecord> { it.timeMillis }.thenByDescending { it.date }
                )
                val bestRecord = sortedByTime.first()
                // Find the fastest time recorded strictly prior to bestRecord (or second fastest overall)
                val distinctFasterOrOlder = sortedByTime
                    .filter { it.id != bestRecord.id }
                    .minByOrNull { it.timeMillis }
                PersonalBestSummary(
                    eventKey = eventKey,
                    record = bestRecord.copy(isPersonalBest = true),
                    previousBestMillis = distinctFasterOrOlder?.timeMillis
                )
            }
            .sortedWith(
                compareBy<PersonalBestSummary> { it.eventKey.stroke.ordinal }
                    .thenBy { it.eventKey.distance.meters }
            )
    }

    /**
     * Computes detailed statistics for a specific [distance] and [stroke], optionally filtered
     * by [poolFilter] and [sessionFilter].
     */
    fun computeEventStatistics(
        allRecords: List<SwimRecord>,
        distance: SwimDistance,
        stroke: Stroke,
        poolFilter: PoolLength? = null,
        sessionFilter: SessionType? = null,
        competitionOnlyPb: Boolean = false,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): EventStatistics {
        val eventKey = EventKey(distance, stroke)
        val matchingEventRecords = allRecords.filter { record ->
            record.distance == distance &&
                record.stroke == stroke &&
                (poolFilter == null || record.poolLength == poolFilter) &&
                (sessionFilter == null || record.sessionType == sessionFilter)
        }

        val validRecords = matchingEventRecords
            .filter { it.status == ResultStatus.FINISHED && it.timeMillis > 0L }
            .sortedWith(compareBy<SwimRecord> { it.date }.thenBy { it.createdAt })

        val pbEligible = validRecords.filter {
            !competitionOnlyPb || it.sessionType == SessionType.COMPETITION
        }

        val pbRecord = pbEligible.minWithOrNull(
            compareBy<SwimRecord> { it.timeMillis }.thenByDescending { it.date }
        )?.copy(isPersonalBest = true)

        val bestTimeMillis = validRecords.minOfOrNull { it.timeMillis }
        val worstTimeMillis = validRecords.maxOfOrNull { it.timeMillis }
        val averageTimeMillis = if (validRecords.isNotEmpty()) {
            validRecords.map { it.timeMillis }.average().roundToLong()
        } else {
            null
        }

        val firstRecord = validRecords.firstOrNull()
        val latestRecord = validRecords.lastOrNull()

        val improvementFromFirst = if (firstRecord != null && pbRecord != null && firstRecord.id != pbRecord.id) {
            // Improvement from first recorded result to PB (or latest)
            SwimTimeUtils.calculateDifference(firstRecord.timeMillis, pbRecord.timeMillis)
        } else if (firstRecord != null && latestRecord != null && firstRecord.id != latestRecord.id) {
            SwimTimeUtils.calculateDifference(firstRecord.timeMillis, latestRecord.timeMillis)
        } else {
            null
        }

        val currentYear = SwimTimeUtils.currentYear()
        val bestThisYearMillis = validRecords
            .filter { SwimTimeUtils.isSameYear(it.date, currentYear) }
            .minOfOrNull { it.timeMillis }

        val bestThisMonthMillis = validRecords
            .filter { SwimTimeUtils.isSameMonthAndYear(it.date, referenceTimeMillis) }
            .minOfOrNull { it.timeMillis }

        val annotatedChronological = validRecords.map { rec ->
            rec.copy(isPersonalBest = pbRecord != null && rec.timeMillis == pbRecord.timeMillis)
        }

        return EventStatistics(
            eventKey = eventKey,
            pbRecord = pbRecord,
            averageTimeMillis = averageTimeMillis,
            bestTimeMillis = bestTimeMillis,
            worstTimeMillis = worstTimeMillis,
            firstRecord = firstRecord,
            latestRecord = latestRecord,
            bestThisMonthMillis = bestThisMonthMillis,
            bestThisYearMillis = bestThisYearMillis,
            totalRecordsCount = matchingEventRecords.size,
            validRecordsCount = validRecords.size,
            improvementFromFirst = improvementFromFirst,
            chronologicalRecords = annotatedChronological
        )
    }

    /**
     * Filters chronological valid records for the progression chart based on [rangeFilter].
     */
    fun filterChartRecords(
        chronologicalRecords: List<SwimRecord>,
        rangeFilter: ChartRangeFilter,
        targetYear: Int = SwimTimeUtils.currentYear()
    ): List<SwimRecord> {
        val valid = chronologicalRecords.filter { it.status == ResultStatus.FINISHED && it.timeMillis > 0L }
        return when (rangeFilter) {
            ChartRangeFilter.ALL -> valid
            ChartRangeFilter.LAST_10 -> valid.takeLast(10)
            ChartRangeFilter.LAST_30 -> valid.takeLast(30)
            ChartRangeFilter.THIS_YEAR -> valid.filter { SwimTimeUtils.isSameYear(it.date, targetYear) }
        }
    }
}
