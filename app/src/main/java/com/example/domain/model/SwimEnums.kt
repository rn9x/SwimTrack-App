package com.example.domain.model

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val isRtl: Boolean
) {
    ENGLISH("en", "English", "English", false),
    ARABIC("ar", "Arabic", "العربية", true);

    companion object {
        fun fromString(value: String): AppLanguage {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.code.equals(value, ignoreCase = true)
            } ?: ENGLISH
        }
    }
}

enum class Stroke(
    val displayName: String,
    val shortName: String,
    val arabicName: String,
    val arabicShortName: String
) {
    FREESTYLE("Freestyle", "Free", "سباحة حرة", "حرة"),
    BACKSTROKE("Backstroke", "Back", "سباحة ظهر", "ظهر"),
    BREASTSTROKE("Breaststroke", "Breast", "سباحة صدر", "صدر"),
    BUTTERFLY("Butterfly", "Fly", "سباحة فراشة", "فراشة"),
    INDIVIDUAL_MEDLEY("Individual Medley", "IM", "فردي متنوع", "متنوع");

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicName else displayName

    fun localizedShortName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicShortName else shortName

    companion object {
        fun fromString(value: String): Stroke {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true) ||
                    it.shortName.equals(value, ignoreCase = true) ||
                    it.arabicName == value
            } ?: FREESTYLE
        }
    }
}

enum class SwimDistance(val meters: Int, val label: String, val arabicLabel: String) {
    D25M(25, "25m", "25م"),
    D50M(50, "50m", "50م"),
    D100M(100, "100m", "100م"),
    D200M(200, "200m", "200م"),
    D400M(400, "400m", "400م"),
    D800M(800, "800m", "800م"),
    D1500M(1500, "1500m", "1500م");

    fun localizedLabel(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicLabel else label

    companion object {
        fun fromMeters(meters: Int): SwimDistance {
            return entries.firstOrNull { it.meters == meters } ?: D50M
        }

        fun fromString(value: String): SwimDistance {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.label.equals(value, ignoreCase = true) ||
                    it.arabicLabel == value ||
                    it.meters.toString() == value.trim().removeSuffix("m").removeSuffix("م")
            } ?: D50M
        }
    }
}

enum class SessionType(val displayName: String, val arabicName: String) {
    TRAINING("Training", "تدريب"),
    COMPETITION("Competition", "بطولة"),
    TIME_TRIAL("Time Trial", "تجربة زمنية");

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicName else displayName

    companion object {
        fun fromString(value: String): SessionType {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true) ||
                    it.arabicName == value
            } ?: TRAINING
        }
    }
}

enum class PoolLength(
    val meters: Int,
    val label: String,
    val arabicLabel: String,
    val courseLabel: String,
    val arabicCourseLabel: String
) {
    POOL_25M(25, "25m", "25م", "25m Pool (SCM)", "مسبح 25م (مجرى قصير)"),
    POOL_50M(50, "50m", "50م", "50m Pool (LCM)", "مسبح 50م (مجرى طويل)");

    fun localizedLabel(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicLabel else label

    fun localizedCourseLabel(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicCourseLabel else courseLabel

    fun localizedBadgeText(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) "مسبح $arabicLabel" else "$label Pool"

    companion object {
        fun fromMeters(meters: Int): PoolLength {
            return if (meters == 50) POOL_50M else POOL_25M
        }

        fun fromString(value: String): PoolLength {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.label.equals(value, ignoreCase = true) ||
                    it.arabicLabel == value ||
                    it.meters.toString() == value.trim().removeSuffix("m").removeSuffix("م")
            } ?: POOL_25M
        }
    }
}

enum class StartType(
    val displayName: String,
    val shortName: String,
    val arabicName: String,
    val arabicShortName: String
) {
    DIVE("Dive Start", "Dive", "قفزة البداية", "قفز"),
    PUSH("Push Start", "Push", "دفع الحائط", "دفع");

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicName else displayName

    fun localizedShortName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicShortName else shortName

    companion object {
        fun fromString(value: String): StartType {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true) ||
                    it.arabicName == value
            } ?: DIVE
        }
    }
}

enum class ResultStatus(
    val displayName: String,
    val arabicName: String,
    val badgeText: String,
    val arabicBadgeText: String,
    val isValidFinish: Boolean
) {
    FINISHED("Finished", "مكتمل", "FIN", "مكتمل", true),
    DQ("Disqualified (DQ)", "استبعاد (DQ)", "DQ", "DQ", false),
    DNS("Did Not Start (DNS)", "لم يبدأ (DNS)", "DNS", "DNS", false),
    NS("No Show (NS)", "لم يحضر (NS)", "NS", "NS", false);

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicName else displayName

    fun localizedBadge(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicBadgeText else badgeText

    companion object {
        fun fromString(value: String): ResultStatus {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.badgeText.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true) ||
                    it.arabicName == value
            } ?: FINISHED
        }
    }
}

enum class ThemePreference(val displayName: String, val arabicName: String) {
    SYSTEM("System Default", "تلقائي (النظام)"),
    LIGHT("Light Mode", "الوضع الفاتح"),
    DARK("Dark Mode", "الوضع الداكن");

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicName else displayName
}

enum class TimeFormatPreference(val displayName: String, val arabicName: String, val example: String) {
    MINUTES_SECONDS("Minutes + Seconds", "دقائق + ثوانٍ", "1:23.45"),
    SECONDS_ONLY("Seconds Only", "ثوانٍ فقط", "83.45");

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicName else displayName
}

enum class ChartRangeFilter(val label: String, val arabicLabel: String) {
    ALL("All Results", "كل النتائج"),
    LAST_10("Last 10", "آخر 10"),
    LAST_30("Last 30", "آخر 30"),
    THIS_YEAR("This Year", "هذا العام");

    fun localizedLabel(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) arabicLabel else label
}
