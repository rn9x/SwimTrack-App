package com.example

import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.SessionType
import com.example.domain.model.StartType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.domain.model.TimeFormatPreference
import com.example.domain.usecase.SwimUseCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun parseAndFormatSwimTime_handlesMinutesSecondsAndCentiseconds() {
        assertEquals(33_200L, SwimTimeUtils.parseSwimTime("33.20"))
        assertEquals("33.20", SwimTimeUtils.formatSwimTime(33_200L))

        assertEquals(83_450L, SwimTimeUtils.parseSwimTime("1:23.45"))
        assertEquals("1:23.45", SwimTimeUtils.formatSwimTime(83_450L))
        assertEquals("83.45", SwimTimeUtils.formatSwimTime(83_450L, TimeFormatPreference.SECONDS_ONLY))

        assertEquals(121_300L, SwimTimeUtils.parseSwimTime("2:01.30"))
        assertEquals("2:01.30", SwimTimeUtils.formatSwimTime(121_300L))

        assertEquals(15_800L, SwimTimeUtils.parseSwimTime("15.80"))
        assertEquals("15.80", SwimTimeUtils.formatSwimTime(15_800L))

        // Invalid inputs
        assertNull(SwimTimeUtils.parseSwimTime(""))
        assertNull(SwimTimeUtils.parseSwimTime("-12.30"))
        assertNull(SwimTimeUtils.parseSwimTime("1:75.00"))
        assertNull(SwimTimeUtils.parseSwimTime("abc"))
    }

    @Test
    fun compareAndCalculateDifference_lowerTimeIsBetter() {
        val oldTime = SwimTimeUtils.parseSwimTime("34.10")!!
        val newTime = SwimTimeUtils.parseSwimTime("33.20")!!

        // Lower time is faster (< 0)
        assertTrue(SwimTimeUtils.compareSwimTimes(newTime, oldTime) < 0)

        val diff = SwimTimeUtils.calculateDifference(oldTime, newTime)
        assertTrue(diff.isImprovement)
        assertEquals(900L, diff.differenceMillis)
        assertEquals("0.90 sec", diff.formattedAbsoluteDifference)
        // ((34100 - 33200) / 34100) * 100 = 2.639... -> 2.64%
        assertEquals("2.64%", diff.formattedPercentage)
    }

    @Test
    fun personalBestDetection_excludesDqDnsNsAndFindsFastestTime() {
        val records = listOf(
            SwimRecord(
                id = 1L,
                date = 1000L,
                timeMillis = 35_200L,
                distance = SwimDistance.D50M,
                stroke = Stroke.FREESTYLE,
                sessionType = SessionType.TRAINING,
                poolLength = PoolLength.POOL_25M,
                startType = StartType.DIVE
            ),
            SwimRecord(
                id = 2L,
                date = 2000L,
                timeMillis = 34_800L,
                distance = SwimDistance.D50M,
                stroke = Stroke.FREESTYLE,
                sessionType = SessionType.TRAINING,
                poolLength = PoolLength.POOL_25M,
                startType = StartType.DIVE
            ),
            SwimRecord(
                id = 3L,
                date = 3000L,
                timeMillis = 33_900L, // True PB (33.90)
                distance = SwimDistance.D50M,
                stroke = Stroke.FREESTYLE,
                sessionType = SessionType.COMPETITION,
                poolLength = PoolLength.POOL_25M,
                startType = StartType.DIVE
            ),
            SwimRecord(
                id = 4L,
                date = 4000L,
                timeMillis = 34_100L,
                distance = SwimDistance.D50M,
                stroke = Stroke.FREESTYLE,
                sessionType = SessionType.TRAINING,
                poolLength = PoolLength.POOL_25M,
                startType = StartType.DIVE
            ),
            SwimRecord(
                id = 5L,
                date = 5000L,
                timeMillis = 31_000L, // Faster numerically, but DQ -> MUST NOT be PB!
                distance = SwimDistance.D50M,
                stroke = Stroke.FREESTYLE,
                sessionType = SessionType.COMPETITION,
                poolLength = PoolLength.POOL_25M,
                startType = StartType.DIVE,
                status = ResultStatus.DQ
            )
        )

        val annotated = SwimUseCases.annotatePersonalBests(records)
        val pbRecord = annotated.firstOrNull { it.isPersonalBest }
        assertNotNull(pbRecord)
        assertEquals(3L, pbRecord?.id)
        assertEquals(33_900L, pbRecord?.timeMillis)
        assertFalse(annotated.first { it.id == 5L }.isPersonalBest)
    }
}
