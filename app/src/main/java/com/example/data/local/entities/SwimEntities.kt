package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.domain.model.Competition
import com.example.domain.model.CompetitionResult
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.SessionType
import com.example.domain.model.StartType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord

@Entity(
    tableName = "swim_records",
    indices = [
        Index(value = ["distance", "stroke", "poolLength"]),
        Index(value = ["date"]),
        Index(value = ["competitionId"])
    ]
)
data class SwimRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: Long,
    val time: Long, // Milliseconds
    val distance: SwimDistance,
    val stroke: Stroke,
    val sessionType: SessionType,
    val poolLength: PoolLength,
    val startType: StartType,
    val competitionId: Long? = null,
    val status: ResultStatus = ResultStatus.FINISHED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(
        competitionName: String? = null,
        isPersonalBest: Boolean = false
    ): SwimRecord = SwimRecord(
        id = id,
        date = date,
        timeMillis = time,
        distance = distance,
        stroke = stroke,
        sessionType = sessionType,
        poolLength = poolLength,
        startType = startType,
        competitionId = competitionId,
        competitionName = competitionName,
        status = status,
        notes = notes,
        createdAt = createdAt,
        isPersonalBest = isPersonalBest
    )

    companion object {
        fun fromDomain(record: SwimRecord): SwimRecordEntity = SwimRecordEntity(
            id = record.id,
            date = record.date,
            time = record.timeMillis,
            distance = record.distance,
            stroke = record.stroke,
            sessionType = record.sessionType,
            poolLength = record.poolLength,
            startType = record.startType,
            competitionId = record.competitionId,
            status = record.status,
            notes = record.notes,
            createdAt = record.createdAt
        )
    }
}

@Entity(
    tableName = "competitions",
    indices = [Index(value = ["date"])]
)
data class CompetitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val date: Long,
    val location: String,
    val notes: String = ""
) {
    fun toDomain(): Competition = Competition(
        id = id,
        name = name,
        date = date,
        location = location,
        notes = notes
    )

    companion object {
        fun fromDomain(competition: Competition): CompetitionEntity = CompetitionEntity(
            id = competition.id,
            name = competition.name,
            date = competition.date,
            location = competition.location,
            notes = competition.notes
        )
    }
}

@Entity(
    tableName = "competition_results",
    indices = [
        Index(value = ["competitionId"]),
        Index(value = ["swimRecordId"])
    ]
)
data class CompetitionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val competitionId: Long,
    val swimRecordId: Long? = null,
    val distance: SwimDistance,
    val stroke: Stroke,
    val poolLength: PoolLength = PoolLength.POOL_50M,
    val startType: StartType = StartType.DIVE,
    val heat: Int? = null,
    val lane: Int? = null,
    val place: Int? = null,
    val seedTime: Long? = null,
    val resultTime: Long? = null,
    val status: ResultStatus = ResultStatus.FINISHED,
    val notes: String = ""
) {
    fun toDomain(isPersonalBest: Boolean = false): CompetitionResult = CompetitionResult(
        id = id,
        competitionId = competitionId,
        swimRecordId = swimRecordId,
        distance = distance,
        stroke = stroke,
        poolLength = poolLength,
        startType = startType,
        heat = heat,
        lane = lane,
        place = place,
        seedTimeMillis = seedTime,
        resultTimeMillis = resultTime,
        status = status,
        notes = notes,
        isPersonalBest = isPersonalBest
    )

    companion object {
        fun fromDomain(result: CompetitionResult): CompetitionResultEntity = CompetitionResultEntity(
            id = result.id,
            competitionId = result.competitionId,
            swimRecordId = result.swimRecordId,
            distance = result.distance,
            stroke = result.stroke,
            poolLength = result.poolLength,
            startType = result.startType,
            heat = result.heat,
            lane = result.lane,
            place = result.place,
            seedTime = result.seedTimeMillis,
            resultTime = result.resultTimeMillis,
            status = result.status,
            notes = result.notes
        )
    }
}

class SwimTypeConverters {
    @TypeConverter
    fun fromStroke(stroke: Stroke): String = stroke.name

    @TypeConverter
    fun toStroke(value: String): Stroke = Stroke.fromString(value)

    @TypeConverter
    fun fromDistance(distance: SwimDistance): String = distance.name

    @TypeConverter
    fun toDistance(value: String): SwimDistance = SwimDistance.fromString(value)

    @TypeConverter
    fun fromSessionType(sessionType: SessionType): String = sessionType.name

    @TypeConverter
    fun toSessionType(value: String): SessionType = SessionType.fromString(value)

    @TypeConverter
    fun fromPoolLength(poolLength: PoolLength): String = poolLength.name

    @TypeConverter
    fun toPoolLength(value: String): PoolLength = PoolLength.fromString(value)

    @TypeConverter
    fun fromStartType(startType: StartType): String = startType.name

    @TypeConverter
    fun toStartType(value: String): StartType = StartType.fromString(value)

    @TypeConverter
    fun fromResultStatus(status: ResultStatus): String = status.name

    @TypeConverter
    fun toResultStatus(value: String): ResultStatus = ResultStatus.fromString(value)
}
