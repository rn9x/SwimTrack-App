package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.local.database.SwimTrackDatabase
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.ChartRangeFilter
import com.example.domain.model.Competition
import com.example.domain.model.CompetitionResult
import com.example.domain.model.CompetitionWithResults
import com.example.domain.model.EventStatistics
import com.example.domain.model.NewPbCelebration
import com.example.domain.model.PersonalBestSummary
import com.example.domain.model.PoolLength
import com.example.domain.model.SessionType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.ThemePreference
import com.example.domain.model.TimeFormatPreference
import com.example.domain.repository.SwimRepository
import com.example.domain.usecase.SwimUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordsFilterState(
    val stroke: Stroke? = null,
    val distance: SwimDistance? = null,
    val sessionType: SessionType? = null,
    val poolLength: PoolLength? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val searchQuery: String = ""
) {
    val hasActiveFilters: Boolean
        get() = stroke != null ||
            distance != null ||
            sessionType != null ||
            poolLength != null ||
            startDateMillis != null ||
            endDateMillis != null ||
            searchQuery.isNotBlank()
}

data class StatisticsFilterState(
    val distance: SwimDistance = SwimDistance.D50M,
    val stroke: Stroke = Stroke.FREESTYLE,
    val sessionType: SessionType? = null,
    val poolLength: PoolLength? = null,
    val chartRange: ChartRangeFilter = ChartRangeFilter.ALL
)

class SwimTrackViewModel(
    private val repository: SwimRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val allRecords: StateFlow<List<SwimRecord>> = combine(
        repository.allRecordsFlow,
        settings
    ) { rawRecords, appSettings ->
        SwimUseCases.annotatePersonalBests(
            records = rawRecords,
            competitionOnlyPb = appSettings.competitionOnlyPb
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val competitionsWithResults: StateFlow<List<CompetitionWithResults>> =
        repository.allCompetitionsWithResultsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val personalBests: StateFlow<List<PersonalBestSummary>> = combine(
        allRecords,
        settings
    ) { records, appSettings ->
        SwimUseCases.computePersonalBests(
            records = records,
            competitionOnlyPb = appSettings.competitionOnlyPb
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Records screen filter state
    private val _recordsFilter = MutableStateFlow(RecordsFilterState())
    val recordsFilter: StateFlow<RecordsFilterState> = _recordsFilter.asStateFlow()

    val filteredRecords: StateFlow<List<SwimRecord>> = combine(
        allRecords,
        _recordsFilter
    ) { records, filter ->
        records.filter { record ->
            val matchStroke = filter.stroke == null || record.stroke == filter.stroke
            val matchDistance = filter.distance == null || record.distance == filter.distance
            val matchSession = filter.sessionType == null || record.sessionType == filter.sessionType
            val matchPool = filter.poolLength == null || record.poolLength == filter.poolLength
            val matchStart = filter.startDateMillis == null || record.date >= filter.startDateMillis
            val matchEnd = filter.endDateMillis == null || record.date <= filter.endDateMillis
            val matchSearch = if (filter.searchQuery.isBlank()) {
                true
            } else {
                val q = filter.searchQuery.trim().lowercase()
                record.eventTitle.lowercase().contains(q) ||
                    record.localizedEventTitle(AppLanguage.ARABIC).lowercase().contains(q) ||
                    record.notes.lowercase().contains(q) ||
                    (record.competitionName?.lowercase()?.contains(q) == true) ||
                    record.poolLength.label.lowercase().contains(q) ||
                    record.poolLength.arabicLabel.contains(q) ||
                    record.sessionType.displayName.lowercase().contains(q) ||
                    record.sessionType.arabicName.contains(q)
            }
            matchStroke && matchDistance && matchSession && matchPool && matchStart && matchEnd && matchSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Statistics screen filter state
    private val _statsFilter = MutableStateFlow(StatisticsFilterState())
    val statsFilter: StateFlow<StatisticsFilterState> = _statsFilter.asStateFlow()

    val currentEventStatistics: StateFlow<EventStatistics> = combine(
        allRecords,
        _statsFilter,
        settings
    ) { records, filter, appSettings ->
        SwimUseCases.computeEventStatistics(
            allRecords = records,
            distance = filter.distance,
            stroke = filter.stroke,
            poolFilter = filter.poolLength,
            sessionFilter = filter.sessionType,
            competitionOnlyPb = appSettings.competitionOnlyPb
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SwimUseCases.computeEventStatistics(
            allRecords = emptyList(),
            distance = SwimDistance.D50M,
            stroke = Stroke.FREESTYLE
        )
    )

    private val _newPbCelebration = MutableStateFlow<NewPbCelebration?>(null)
    val newPbCelebration: StateFlow<NewPbCelebration?> = _newPbCelebration.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun dismissPbCelebration() {
        _newPbCelebration.value = null
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    // --- Record Filter Actions ---
    fun updateRecordsStrokeFilter(stroke: Stroke?) {
        _recordsFilter.update { it.copy(stroke = stroke) }
    }

    fun updateRecordsDistanceFilter(distance: SwimDistance?) {
        _recordsFilter.update { it.copy(distance = distance) }
    }

    fun updateRecordsSessionFilter(sessionType: SessionType?) {
        _recordsFilter.update { it.copy(sessionType = sessionType) }
    }

    fun updateRecordsPoolFilter(poolLength: PoolLength?) {
        _recordsFilter.update { it.copy(poolLength = poolLength) }
    }

    fun updateRecordsDateRange(startMillis: Long?, endMillis: Long?) {
        _recordsFilter.update { it.copy(startDateMillis = startMillis, endDateMillis = endMillis) }
    }

    fun updateRecordsSearchQuery(query: String) {
        _recordsFilter.update { it.copy(searchQuery = query) }
    }

    fun clearRecordsFilters() {
        _recordsFilter.value = RecordsFilterState()
    }

    // --- Statistics Filter Actions ---
    fun selectStatsEvent(distance: SwimDistance, stroke: Stroke) {
        _statsFilter.update { it.copy(distance = distance, stroke = stroke) }
    }

    fun updateStatsSessionFilter(sessionType: SessionType?) {
        _statsFilter.update { it.copy(sessionType = sessionType) }
    }

    fun updateStatsPoolFilter(poolLength: PoolLength?) {
        _statsFilter.update { it.copy(poolLength = poolLength) }
    }

    fun updateStatsChartRange(range: ChartRangeFilter) {
        _statsFilter.update { it.copy(chartRange = range) }
    }

    // --- Record CRUD ---
    fun saveRecord(record: SwimRecord, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val lang = settings.value.language
            val result = repository.saveSwimRecord(
                record = record,
                competitionOnlyPb = settings.value.competitionOnlyPb
            )
            if (result.newPbCelebration != null) {
                _newPbCelebration.value = result.newPbCelebration
            } else {
                _snackbarMessage.value = if (lang == AppLanguage.ARABIC) {
                    if (record.id == 0L) "تم حفظ السجل (${record.localizedEventTitle(lang)})" else "تم تحديث السجل"
                } else {
                    if (record.id == 0L) "Swim record saved (${record.eventTitle})" else "Swim record updated"
                }
            }
            onSaved()
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            val lang = settings.value.language
            repository.deleteSwimRecord(recordId)
            _snackbarMessage.value = if (lang == AppLanguage.ARABIC) "تم حذف السجل" else "Record deleted"
        }
    }

    // --- Competition CRUD ---
    fun saveCompetition(competition: Competition, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val lang = settings.value.language
            val id = repository.saveCompetition(competition)
            _snackbarMessage.value = if (lang == AppLanguage.ARABIC) {
                if (competition.id == 0L) "تم إنشاء البطولة \"${competition.name}\"" else "تم تحديث البطولة"
            } else {
                if (competition.id == 0L) "Competition \"${competition.name}\" created" else "Competition updated"
            }
            onSaved(id)
        }
    }

    fun deleteCompetition(competitionId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val lang = settings.value.language
            repository.deleteCompetition(competitionId)
            _snackbarMessage.value = if (lang == AppLanguage.ARABIC) "تم حذف البطولة" else "Competition deleted"
            onDeleted()
        }
    }

    fun saveCompetitionResult(result: CompetitionResult, competitionDate: Long, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val lang = settings.value.language
            val celebration = repository.saveCompetitionResult(
                result = result,
                competitionDate = competitionDate,
                competitionOnlyPb = settings.value.competitionOnlyPb
            )
            if (celebration != null) {
                _newPbCelebration.value = celebration
            } else {
                _snackbarMessage.value = if (lang == AppLanguage.ARABIC) {
                    "تم حفظ نتيجة السباق (${result.localizedEventTitle(lang)})"
                } else {
                    "Event result saved (${result.eventTitle})"
                }
            }
            onSaved()
        }
    }

    fun deleteCompetitionResult(result: CompetitionResult) {
        viewModelScope.launch {
            val lang = settings.value.language
            repository.deleteCompetitionResult(result)
            _snackbarMessage.value = if (lang == AppLanguage.ARABIC) "تم حذف سباق البطولة" else "Competition event removed"
        }
    }

    // --- Settings & Data Management ---
    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsDataStore.updateLanguage(language)
        }
    }

    fun updateThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            settingsDataStore.updateTheme(theme)
        }
    }

    fun updateTimeFormatPreference(format: TimeFormatPreference) {
        viewModelScope.launch {
            settingsDataStore.updateTimeFormat(format)
        }
    }

    fun updateDefaultPoolLength(poolLength: PoolLength) {
        viewModelScope.launch {
            settingsDataStore.updateDefaultPool(poolLength)
        }
    }

    fun updateDefaultSessionType(sessionType: SessionType) {
        viewModelScope.launch {
            settingsDataStore.updateDefaultSessionType(sessionType)
        }
    }

    fun updateCompetitionOnlyPb(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateCompetitionOnlyPb(enabled)
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            val lang = settings.value.language
            repository.loadDemoSampleData()
            _snackbarMessage.value = if (lang == AppLanguage.ARABIC) {
                "تم تحميل البيانات التجريبية بنجاح!"
            } else {
                "Demo sample data loaded! You can clear it anytime in Settings."
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val lang = settings.value.language
            repository.deleteAllData()
            _snackbarMessage.value = if (lang == AppLanguage.ARABIC) {
                "تم حذف جميع سجلات السباحة والبطولات."
            } else {
                "All swimming records and competitions deleted."
            }
        }
    }

    suspend fun exportJsonString(): String {
        return repository.exportDataToJson()
    }

    fun importJsonString(jsonString: String) {
        viewModelScope.launch {
            val lang = settings.value.language
            val result = repository.importDataFromJson(jsonString)
            result.fold(
                onSuccess = { count ->
                    _snackbarMessage.value = if (lang == AppLanguage.ARABIC) {
                        "تم استيراد $count عنصر من النسخة الاحتياطية بنجاح."
                    } else {
                        "Successfully imported $count items from backup."
                    }
                },
                onFailure = { error ->
                    _snackbarMessage.value = if (lang == AppLanguage.ARABIC) {
                        "فشل الاستيراد: ملف النسخة الاحتياطية غير صالح."
                    } else {
                        error.message ?: "Import failed: invalid backup file."
                    }
                }
            )
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = SwimTrackDatabase.getInstance(context)
                    val repository = SwimRepository(database.swimTrackDao())
                    val settingsDataStore = SettingsDataStore(context.applicationContext)
                    return SwimTrackViewModel(repository, settingsDataStore) as T
                }
            }
        }
    }
}
