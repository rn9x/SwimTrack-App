package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.SwimTrackDatabase
import com.example.domain.model.Competition
import com.example.domain.model.CompetitionResult
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.SessionType
import com.example.domain.model.StartType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.repository.SwimRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: SwimTrackDatabase
    private lateinit var repository: SwimRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SwimTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SwimRepository(database.swimTrackDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun appName_isSwimTrack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SwimTrack", appName)
    }

    @Test
    fun crudRecordAndPbDetection_worksCorrectly() = runTest {
        // 1. Add first 50m Free record (34.10)
        val rec1 = SwimRecord(
            date = 1_700_000_000_000L,
            timeMillis = 34_100L,
            distance = SwimDistance.D50M,
            stroke = Stroke.FREESTYLE,
            sessionType = SessionType.TRAINING,
            poolLength = PoolLength.POOL_25M,
            startType = StartType.DIVE,
            notes = "First attempt"
        )
        val save1 = repository.saveSwimRecord(rec1)
        assertNotNull(save1.newPbCelebration)
        assertNull(save1.newPbCelebration?.previousPbMillis)

        // 2. Add faster 50m Free record (33.20) -> New PB with 0.90s improvement
        val rec2 = rec1.copy(
            date = 1_700_086_400_000L,
            timeMillis = 33_200L,
            notes = "Faster attempt"
        )
        val save2 = repository.saveSwimRecord(rec2)
        assertNotNull(save2.newPbCelebration)
        assertEquals(34_100L, save2.newPbCelebration?.previousPbMillis)
        assertEquals(33_200L, save2.newPbCelebration?.newPbMillis)
        assertEquals(900L, save2.newPbCelebration?.improvementMillis)

        // 3. Edit record 2 to 33.00
        val editedRec2 = repository.getRecordById(save2.recordId)!!.copy(timeMillis = 33_000L)
        repository.saveSwimRecord(editedRec2)
        val allAfterEdit = repository.allRecordsFlow.first()
        assertEquals(2, allAfterEdit.size)
        val currentPb = allAfterEdit.first { it.isPersonalBest }
        assertEquals(33_000L, currentPb.timeMillis)

        // 4. Delete record 2 -> Record 1 (34.10) becomes PB again
        repository.deleteSwimRecord(save2.recordId)
        val allAfterDelete = repository.allRecordsFlow.first()
        assertEquals(1, allAfterDelete.size)
        assertTrue(allAfterDelete.first().isPersonalBest)
        assertEquals(34_100L, allAfterDelete.first().timeMillis)
    }

    @Test
    fun competitionResults_dqDoesNotBecomePb_andJsonExportImportWorks() = runTest {
        val compId = repository.saveCompetition(
            Competition(
                name = "Giza Summer Championship 2026",
                date = 1_700_000_000_000L,
                location = "Giza Pool"
            )
        )

        // Save DQ competition result with fast split
        val dqCelebration = repository.saveCompetitionResult(
            result = CompetitionResult(
                competitionId = compId,
                distance = SwimDistance.D50M,
                stroke = Stroke.BUTTERFLY,
                poolLength = PoolLength.POOL_50M,
                heat = 2,
                lane = 4,
                resultTimeMillis = 28_500L,
                status = ResultStatus.DQ
            ),
            competitionDate = 1_700_000_000_000L
        )
        assertNull(dqCelebration)

        // Save valid FINISHED competition result
        val validCelebration = repository.saveCompetitionResult(
            result = CompetitionResult(
                competitionId = compId,
                distance = SwimDistance.D50M,
                stroke = Stroke.BUTTERFLY,
                poolLength = PoolLength.POOL_50M,
                heat = 3,
                lane = 5,
                place = 1,
                seedTimeMillis = 31_000L,
                resultTimeMillis = 30_200L,
                status = ResultStatus.FINISHED
            ),
            competitionDate = 1_700_000_000_000L
        )
        assertNotNull(validCelebration)
        assertEquals(30_200L, validCelebration?.newPbMillis)

        // Export JSON, clear DB, and Import JSON
        val exportedJson = repository.exportDataToJson()
        assertTrue(exportedJson.contains("Giza Summer Championship 2026"))

        repository.deleteAllData()
        assertEquals(0, repository.allRecordsFlow.first().size)

        val importResult = repository.importDataFromJson(exportedJson)
        assertTrue(importResult.isSuccess)
        assertEquals(2, repository.allRecordsFlow.first().size)
        assertEquals(1, repository.allCompetitionsWithResultsFlow.first().size)
    }
}
