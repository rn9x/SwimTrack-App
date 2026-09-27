package com.example.ui.navigation

import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance

object SwimRoutes {
    const val HOME = "Home"
    const val RECORDS = "Records"
    const val ADD_RECORD = "AddRecord"
    const val EDIT_RECORD = "AddRecord?recordId={recordId}"
    const val COMPETITIONS = "Competitions"
    const val COMPETITION_DETAILS = "CompetitionDetails/{competitionId}"
    const val STATISTICS = "Statistics"
    const val EVENT_DETAILS = "EventDetails/{distance}/{stroke}"
    const val SETTINGS = "Settings"

    fun editRecordRoute(recordId: Long? = null): String {
        return if (recordId != null && recordId > 0L) {
            "AddRecord?recordId=$recordId"
        } else {
            ADD_RECORD
        }
    }

    fun competitionDetailsRoute(competitionId: Long): String =
        "CompetitionDetails/$competitionId"

    fun eventDetailsRoute(distance: SwimDistance, stroke: Stroke): String =
        "EventDetails/${distance.name}/${stroke.name}"
}
