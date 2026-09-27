package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class TimeComparison(
    val oldTimeMillis: Long,
    val newTimeMillis: Long,
    val differenceMillis: Long, // positive = faster (improvement), negative = slower
    val percentageImprovement: Double, // ((oldTime - newTime) / oldTime) * 100
    val isImprovement: Boolean
) {
    val formattedDifferenceSeconds: String
        get() = SwimTimeUtils.formatSignedDifference(differenceMillis)

    val formattedAbsoluteDifference: String
        get() = SwimTimeUtils.formatAbsoluteDifference(differenceMillis)

    val formattedPercentage: String
        get() = String.format(Locale.US, "%.2f%%", percentageImprovement)

    val formattedAbsolutePercentage: String
        get() = String.format(Locale.US, "%.2f%%", abs(percentageImprovement))
}

object SwimTimeUtils {

    /**
     * Formats milliseconds into swimming time representation with centisecond precision.
     * Examples:
     * 33200L -> "33.20"
     * 83450L -> "1:23.45"
     * 121300L -> "2:01.30"
     * 15800L -> "15.80"
     *
     * When [timeFormat] is [TimeFormatPreference.SECONDS_ONLY], 83450L -> "83.45".
     */
    fun formatSwimTime(
        timeMillis: Long,
        timeFormat: TimeFormatPreference = TimeFormatPreference.MINUTES_SECONDS
    ): String {
        if (timeMillis <= 0L) return "—"
        val totalCentiseconds = (timeMillis / 10.0).roundToLong()
        val centiseconds = (totalCentiseconds % 100).toInt()
        val totalSeconds = totalCentiseconds / 100

        if (timeFormat == TimeFormatPreference.SECONDS_ONLY || totalSeconds < 60) {
            return String.format(Locale.US, "%d.%02d", totalSeconds, centiseconds)
        }

        val minutes = totalSeconds / 60
        val seconds = (totalSeconds % 60).toInt()
        return String.format(Locale.US, "%d:%02d.%02d", minutes, seconds, centiseconds)
    }

    /**
     * Parses a swimming time string into milliseconds.
     * Supports:
     * - "33.20" -> 33200L
     * - "1:23.45" -> 83450L
     * - "2:01.30" -> 121300L
     * - "15.80" -> 15800L
     * - "83.45" -> 83450L
     * - "1:05.2" -> 65200L
     * - "45" -> 45000L
     * Returns null if invalid or <= 0.
     */
    fun parseSwimTime(input: String): Long? {
        // Normalize any Arabic-Indic digits or Arabic decimal separators to standard ASCII
        val normalized = input.trim()
            .map { ch ->
                when (ch) {
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    '٫', ',' -> '.'
                    else -> ch
                }
            }
            .joinToString("")
        val trimmed = normalized.trim()
        if (trimmed.isEmpty()) return null

        // Reject negative numbers or invalid characters
        if (trimmed.startsWith("-")) return null

        val colonParts = trimmed.split(":")
        if (colonParts.size > 2) return null

        return try {
            val minutes: Long
            val secondsPart: String

            if (colonParts.size == 2) {
                val minStr = colonParts[0].trim()
                if (minStr.isEmpty() || !minStr.all { it.isDigit() }) return null
                minutes = minStr.toLong()
                secondsPart = colonParts[1].trim()
            } else {
                minutes = 0L
                secondsPart = colonParts[0].trim()
            }

            if (secondsPart.isEmpty()) return null
            val dotParts = secondsPart.split(".")
            if (dotParts.size > 2) return null

            val wholeSecondsStr = dotParts[0]
            if (wholeSecondsStr.isEmpty() || !wholeSecondsStr.all { it.isDigit() }) return null
            val wholeSeconds = wholeSecondsStr.toLong()

            // When minutes are explicitly provided (MM:SS.cc), seconds must be < 60
            if (colonParts.size == 2 && wholeSeconds >= 60L) return null

            val centiseconds: Long = if (dotParts.size == 2) {
                val fracStr = dotParts[1]
                if (fracStr.isEmpty() || !fracStr.all { it.isDigit() } || fracStr.length > 3) {
                    return null
                }
                when (fracStr.length) {
                    1 -> fracStr.toLong() * 100L // .2 -> 200ms
                    2 -> fracStr.toLong() * 10L  // .20 -> 200ms
                    3 -> fracStr.toLong()        // .205 -> 205ms
                    else -> return null
                }
            } else {
                0L
            }

            val totalMillis = (minutes * 60_000L) + (wholeSeconds * 1_000L) + centiseconds
            if (totalMillis <= 0L) null else totalMillis
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Converts raw digit-only quick keypad input into a formatted swim time string.
     * Examples:
     * "3320" -> "33.20"
     * "12345" -> "1:23.45"
     * "20130" -> "2:01.30"
     * "1580" -> "15.80"
     */
    fun formatKeypadDigits(digits: String): String {
        val clean = digits.filter { it.isDigit() }.trimStart('0')
        if (clean.isEmpty()) return ""
        val padded = clean.padStart(3, '0')
        val centi = padded.takeLast(2)
        val rest = padded.dropLast(2)
        return if (rest.length <= 2) {
            "$rest.$centi"
        } else {
            val sec = rest.takeLast(2)
            val min = rest.dropLast(2)
            "$min:$sec.$centi"
        }
    }

    /**
     * Compares two valid swim times in milliseconds.
     * Since lower time = better performance in swimming:
     * Returns negative if [timeA] is faster (better) than [timeB],
     * 0 if equal, positive if [timeA] is slower than [timeB].
     */
    fun compareSwimTimes(timeA: Long, timeB: Long): Int {
        return timeA.compareTo(timeB)
    }

    /**
     * Calculates the time difference and percentage improvement between [oldTimeMillis] and [newTimeMillis].
     * Example:
     * oldTime = 34.10 (34100ms), newTime = 33.20 (33200ms)
     * difference = 900ms (0.90 sec improvement)
     * percentageImprovement = ((34100 - 33200) / 34100) * 100 = 2.64%
     */
    fun calculateDifference(oldTimeMillis: Long, newTimeMillis: Long): TimeComparison {
        val diffMillis = oldTimeMillis - newTimeMillis
        val percentage = if (oldTimeMillis > 0L) {
            ((oldTimeMillis - newTimeMillis).toDouble() / oldTimeMillis.toDouble()) * 100.0
        } else {
            0.0
        }
        return TimeComparison(
            oldTimeMillis = oldTimeMillis,
            newTimeMillis = newTimeMillis,
            differenceMillis = diffMillis,
            percentageImprovement = percentage,
            isImprovement = diffMillis > 0L
        )
    }

    fun formatAbsoluteDifference(
        diffMillis: Long,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String {
        val absCentiseconds = (abs(diffMillis) / 10.0).roundToLong()
        val centiseconds = (absCentiseconds % 100).toInt()
        val totalSeconds = absCentiseconds / 100
        val secUnit = if (language == AppLanguage.ARABIC) "ث" else "sec"
        return if (totalSeconds < 60) {
            String.format(Locale.US, "%d.%02d %s", totalSeconds, centiseconds, secUnit)
        } else {
            val minutes = totalSeconds / 60
            val seconds = (totalSeconds % 60).toInt()
            String.format(Locale.US, "%d:%02d.%02d", minutes, seconds, centiseconds)
        }
    }

    fun formatSignedDifference(diffMillis: Long): String {
        if (diffMillis == 0L) return "0.00s"
        val absCentiseconds = (abs(diffMillis) / 10.0).roundToLong()
        val centiseconds = (absCentiseconds % 100).toInt()
        val totalSeconds = absCentiseconds / 100
        val sign = if (diffMillis > 0L) "-" else "+" // -0.90s means 0.90s faster!
        return if (totalSeconds < 60) {
            String.format(Locale.US, "%s%d.%02d sec", sign, totalSeconds, centiseconds)
        } else {
            val minutes = totalSeconds / 60
            val seconds = (totalSeconds % 60).toInt()
            String.format(Locale.US, "%s%d:%02d.%02d", sign, minutes, seconds, centiseconds)
        }
    }

    fun formatDateShort(
        epochMillis: Long,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String {
        val locale = if (language == AppLanguage.ARABIC) Locale.forLanguageTag("ar-EG-u-nu-latn") else Locale.US
        val sdf = SimpleDateFormat("dd MMM yyyy", locale)
        return sdf.format(Date(epochMillis))
    }

    fun formatDateCompact(
        epochMillis: Long,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String {
        val locale = if (language == AppLanguage.ARABIC) Locale.forLanguageTag("ar-EG-u-nu-latn") else Locale.US
        val sdf = SimpleDateFormat("dd MMM", locale)
        return sdf.format(Date(epochMillis))
    }

    fun isSameYear(epochMillis: Long, targetYear: Int = currentYear()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return cal.get(Calendar.YEAR) == targetYear
    }

    fun isSameMonthAndYear(epochMillis: Long, referenceMillis: Long = System.currentTimeMillis()): Boolean {
        val refCal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return cal.get(Calendar.YEAR) == refCal.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == refCal.get(Calendar.MONTH)
    }

    fun currentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }
}
