package com.example.domain.model

data class EventKey(
    val distance: SwimDistance,
    val stroke: Stroke
) {
    val displayName: String
        get() = "${distance.label} ${stroke.displayName}"

    val shortDisplayName: String
        get() = "${distance.label} ${stroke.shortName}"

    fun localizedDisplayName(language: AppLanguage): String =
        "${distance.localizedLabel(language)} ${stroke.localizedName(language)}"

    fun localizedName(language: AppLanguage): String =
        localizedDisplayName(language)
}

data class SwimRecord(
    val id: Long = 0L,
    val date: Long,
    val timeMillis: Long,
    val distance: SwimDistance,
    val stroke: Stroke,
    val sessionType: SessionType,
    val poolLength: PoolLength,
    val startType: StartType,
    val competitionId: Long? = null,
    val competitionName: String? = null,
    val status: ResultStatus = ResultStatus.FINISHED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPersonalBest: Boolean = false
) {
    val eventKey: EventKey
        get() = EventKey(distance, stroke)

    val eventTitle: String
        get() = "${distance.label} ${stroke.displayName}"

    fun localizedEventTitle(language: AppLanguage): String =
        "${distance.localizedLabel(language)} ${stroke.localizedName(language)}"

    val isValidForPb: Boolean
        get() = status.isValidFinish && timeMillis > 0L
}

data class Competition(
    val id: Long = 0L,
    val name: String,
    val date: Long,
    val location: String,
    val notes: String = ""
)

data class CompetitionResult(
    val id: Long = 0L,
    val competitionId: Long,
    val swimRecordId: Long? = null,
    val distance: SwimDistance,
    val stroke: Stroke,
    val poolLength: PoolLength = PoolLength.POOL_50M,
    val startType: StartType = StartType.DIVE,
    val heat: Int? = null,
    val lane: Int? = null,
    val place: Int? = null,
    val seedTimeMillis: Long? = null,
    val resultTimeMillis: Long? = null,
    val status: ResultStatus = ResultStatus.FINISHED,
    val notes: String = "",
    val isPersonalBest: Boolean = false
) {
    val eventKey: EventKey
        get() = EventKey(distance, stroke)

    val eventTitle: String
        get() = "${distance.label} ${stroke.displayName}"

    fun localizedEventTitle(language: AppLanguage): String =
        "${distance.localizedLabel(language)} ${stroke.localizedName(language)}"
}

data class CompetitionWithResults(
    val competition: Competition,
    val results: List<CompetitionResult>
)

data class PersonalBestSummary(
    val eventKey: EventKey,
    val record: SwimRecord,
    val previousBestMillis: Long? = null
) {
    val improvementComparison: TimeComparison?
        get() = previousBestMillis?.let { SwimTimeUtils.calculateDifference(it, record.timeMillis) }
}

data class NewPbCelebration(
    val eventTitle: String,
    val distance: SwimDistance = SwimDistance.D50M,
    val stroke: Stroke = Stroke.FREESTYLE,
    val poolLength: PoolLength,
    val newPbMillis: Long,
    val previousPbMillis: Long?,
    val improvementMillis: Long?
) {
    fun localizedEventTitle(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) {
            "${distance.localizedLabel(language)} ${stroke.localizedName(language)}"
        } else {
            eventTitle
        }
}

data class EventStatistics(
    val eventKey: EventKey,
    val pbRecord: SwimRecord?,
    val averageTimeMillis: Long?,
    val bestTimeMillis: Long?,
    val worstTimeMillis: Long?,
    val firstRecord: SwimRecord?,
    val latestRecord: SwimRecord?,
    val bestThisMonthMillis: Long?,
    val bestThisYearMillis: Long?,
    val totalRecordsCount: Int,
    val validRecordsCount: Int,
    val improvementFromFirst: TimeComparison?,
    val chronologicalRecords: List<SwimRecord> // oldest to newest for charts
)

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val timeFormatPreference: TimeFormatPreference = TimeFormatPreference.MINUTES_SECONDS,
    val defaultPoolLength: PoolLength = PoolLength.POOL_25M,
    val defaultSessionType: SessionType = SessionType.TRAINING,
    val competitionOnlyPb: Boolean = false
)
