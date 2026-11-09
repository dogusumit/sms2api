package com.sms2api.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sms2api.app.SMS2ApiApplication
import com.sms2api.app.data.model.AuthType
import com.sms2api.app.data.model.DeliveryStatus
import com.sms2api.app.data.model.FilterMode
import com.sms2api.app.data.model.HeaderItem
import com.sms2api.app.data.model.LogFilterCategory
import com.sms2api.app.data.model.SmsRecord
import com.sms2api.app.service.SmsForwarderService
import com.sms2api.app.worker.HistoricalImportWorker
import com.sms2api.app.worker.SmsForwardWorker
import com.sms2api.app.util.LocaleHelper
import com.sms2api.app.util.UrlValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val matchedCount: Int = 0,
    val skippedCount: Int = 0,
    val records: List<SmsRecord> = emptyList(),
    val filteredRecords: List<SmsRecord> = emptyList(),
    val selectedFilterStatus: DeliveryStatus? = null,
    val selectedFilterCategory: LogFilterCategory = LogFilterCategory.ALL,
    val searchQuery: String = "",
    val isServiceRunning: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val apiEndpoint: String = "",
    val authType: AuthType = AuthType.NONE,
    val basicUsername: String = "",
    val basicPassword: String = "",
    val bearerToken: String = "",
    val customHeaders: List<HeaderItem> = emptyList(),
    val filterMode: FilterMode = FilterMode.ALL,
    val whitelistNumbers: String = "",
    val keywordFilter: String = "",
    val isBootAutostartEnabled: Boolean = true,
    val isPeriodicSyncEnabled: Boolean = true,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    val isHistoricalImporting: Boolean = false,
    val importResultSummary: String? = null,
    val snackbarMessage: String? = null,
    val appLanguage: String = "",
    val sendingMessageIds: Set<String> = emptySet(),
    val hasSettingsChanged: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SMS2ApiApplication
    private val repository = app.repository
    private val prefs = app.preferencesManager
    private val apiClient = app.apiClient

    private val _uiState = MutableStateFlow(
        MainUiState(
            isServiceRunning = prefs.isServiceEnabled,
            apiEndpoint = prefs.apiEndpoint,
            authType = prefs.authType,
            basicUsername = prefs.basicUsername,
            basicPassword = prefs.basicPassword,
            bearerToken = prefs.bearerToken,
            customHeaders = if (prefs.customHeaders.isEmpty()) listOf(HeaderItem("X-API-Key", "")) else prefs.customHeaders,
            filterMode = prefs.filterMode,
            whitelistNumbers = prefs.whitelistNumbers,
            keywordFilter = prefs.keywordFilter,
            isBootAutostartEnabled = prefs.isBootAutostartEnabled,
            isPeriodicSyncEnabled = prefs.isPeriodicSyncEnabled,
            appLanguage = prefs.appLanguage
        )
    )

    val uiState: StateFlow<MainUiState> = combine(
        _uiState,
        repository.allRecordsFlow
    ) { state, allRecords ->
        val total = allRecords.size
        val pending = allRecords.count { it.status == DeliveryStatus.PENDING.name }
        val success = allRecords.count { it.status == DeliveryStatus.SUCCESS.name }
        val failed = allRecords.count { it.status == DeliveryStatus.FAILED.name }

        val matched = allRecords.count { repository.matchesFilter(it.sender, it.body, application) }
        val skipped = allRecords.count { !repository.matchesFilter(it.sender, it.body, application) }

        val filtered = allRecords.filter { record ->
            val isFilterMatch = repository.matchesFilter(record.sender, record.body, application)
            val matchesCategory = when (state.selectedFilterCategory) {
                LogFilterCategory.ALL -> true
                LogFilterCategory.MATCHED -> isFilterMatch
                LogFilterCategory.SKIPPED -> !isFilterMatch
                LogFilterCategory.SUCCESS -> record.status == DeliveryStatus.SUCCESS.name
                LogFilterCategory.PENDING -> record.status == DeliveryStatus.PENDING.name
                LogFilterCategory.FAILED -> record.status == DeliveryStatus.FAILED.name
            }
            val matchesQuery = if (state.searchQuery.isBlank()) {
                true
            } else {
                val q = state.searchQuery.trim().lowercase()
                record.sender.lowercase().contains(q) ||
                record.body.lowercase().contains(q) ||
                record.messageId.lowercase().contains(q)
            }
            matchesCategory && matchesQuery
        }

        state.copy(
            totalCount = total,
            pendingCount = pending,
            successCount = success,
            failedCount = failed,
            matchedCount = matched,
            skippedCount = skipped,
            records = allRecords,
            filteredRecords = filtered
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)

    fun setFilterCategory(category: LogFilterCategory) {
        _uiState.update { it.copy(selectedFilterCategory = category) }
    }

    init {
        checkBatteryOptimization(application)
    }

    fun checkBatteryOptimization(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        _uiState.update { it.copy(isBatteryOptimizationIgnored = isIgnored) }
    }

    fun toggleService(context: Context) {
        val nextState = !_uiState.value.isServiceRunning
        prefs.isServiceEnabled = nextState
        _uiState.update { it.copy(isServiceRunning = nextState) }

        if (nextState) {
            SmsForwarderService.start(context)
            _uiState.update { it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.snack_service_started)) }
        } else {
            SmsForwarderService.stop(context)
            _uiState.update { it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.snack_service_stopped)) }
        }
    }

    fun refreshServiceStatus() {
        _uiState.update { it.copy(isServiceRunning = prefs.isServiceEnabled) }
    }

    fun flushQueue(context: Context) {
        viewModelScope.launch {
            SmsForwardWorker.enqueue(context)
            _uiState.update { it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.snack_queue_triggered)) }
        }
    }

    fun retryRecord(messageId: String) {
        viewModelScope.launch {
            val success = repository.dispatchSingleRecord(messageId)
            val msg = if (success) {
                app.getString(com.sms2api.app.R.string.snack_delivered_success)
            } else {
                app.getString(com.sms2api.app.R.string.snack_delivered_failed)
            }
            _uiState.update { it.copy(snackbarMessage = msg) }
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            val count = repository.retryAllFailed()
            app.setupPeriodicSyncWorker()
            SmsForwardWorker.enqueue(app)
            _uiState.update { it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.snack_retry_failed_queued, count)) }
        }
    }

    fun resendAllRecords(context: Context) {
        viewModelScope.launch {
            repository.resetAllToPending()
            SmsForwardWorker.enqueue(context)
            _uiState.update { it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.snack_queued_all_resend)) }
        }
    }

    fun clearSuccessful() {
        viewModelScope.launch {
            repository.clearSuccessful()
            _uiState.update { it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.snack_cleared_delivered)) }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _uiState.update { it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.snack_cleared_all)) }
        }
    }

    fun importHistoricalMessages(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isHistoricalImporting = true) }
            val result = repository.importHistoricalInbox(context)
            _uiState.update { it.copy(isHistoricalImporting = false) }

            if (result.success) {
                val summary = app.getString(
                    com.sms2api.app.R.string.snack_historical_completed,
                    result.totalScanned,
                    result.matched,
                    result.newlyQueued
                )
                _uiState.update {
                    it.copy(
                        importResultSummary = summary,
                        snackbarMessage = summary
                    )
                }
                if (result.newlyQueued > 0) {
                    SmsForwardWorker.enqueue(context)
                }
            } else {
                val err = app.getString(com.sms2api.app.R.string.snack_historical_failed, result.errorMessage ?: "")
                _uiState.update {
                    it.copy(snackbarMessage = err)
                }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
            val result = apiClient.testConnection()
            val text = if (result.isSuccess) {
                app.getString(com.sms2api.app.R.string.conn_test_success, result.httpCode ?: 200)
            } else {
                app.getString(com.sms2api.app.R.string.conn_test_failed, result.errorMessage ?: "HTTP ${result.httpCode}")
            }
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    connectionTestResult = text,
                    snackbarMessage = text
                )
            }
        }
    }

    fun setFilterStatus(status: DeliveryStatus?) {
        _uiState.update { it.copy(selectedFilterStatus = status) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateApiEndpoint(url: String) {
        _uiState.update { it.copy(apiEndpoint = url) }
    }

    fun updateAuthType(type: AuthType) {
        _uiState.update { it.copy(authType = type) }
    }

    fun updateBasicUsername(user: String) {
        _uiState.update { it.copy(basicUsername = user) }
    }

    fun updateBasicPassword(pass: String) {
        _uiState.update { it.copy(basicPassword = pass) }
    }

    fun updateBearerToken(token: String) {
        _uiState.update { it.copy(bearerToken = token) }
    }

    fun updateCustomHeaders(headers: List<HeaderItem>) {
        _uiState.update { it.copy(customHeaders = headers) }
    }

    fun addCustomHeader() {
        _uiState.update {
            val list = it.customHeaders.toMutableList()
            list.add(HeaderItem())
            it.copy(customHeaders = list)
        }
    }

    fun removeCustomHeader(index: Int) {
        _uiState.update {
            val list = it.customHeaders.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            it.copy(customHeaders = list)
        }
    }

    fun updateCustomHeaderItem(index: Int, key: String, value: String) {
        _uiState.update {
            val list = it.customHeaders.toMutableList()
            if (index in list.indices) {
                list[index] = HeaderItem(key, value)
            }
            it.copy(customHeaders = list)
        }
    }

    fun updateFilterMode(mode: FilterMode) {
        prefs.filterMode = mode
        _uiState.update { it.copy(filterMode = mode) }
    }

    fun updateWhitelistNumbers(numbers: String) {
        prefs.whitelistNumbers = numbers
        _uiState.update { it.copy(whitelistNumbers = numbers) }
    }

    fun updateKeywordFilter(keyword: String) {
        prefs.keywordFilter = keyword
        _uiState.update { it.copy(keywordFilter = keyword) }
    }

    fun updateBootAutostart(enabled: Boolean) {
        prefs.isBootAutostartEnabled = enabled
        _uiState.update { it.copy(isBootAutostartEnabled = enabled) }
    }

    fun updatePeriodicSync(enabled: Boolean) {
        prefs.isPeriodicSyncEnabled = enabled
        _uiState.update { it.copy(isPeriodicSyncEnabled = enabled) }
        app.setupPeriodicSyncWorker()
    }

    fun saveSettings() {
        val s = _uiState.value
        val trimmedEndpoint = s.apiEndpoint.trim()

        if (trimmedEndpoint.isNotEmpty() && !UrlValidator.isValidUrl(trimmedEndpoint)) {
            _uiState.update {
                it.copy(snackbarMessage = app.getString(com.sms2api.app.R.string.err_invalid_url_format))
            }
            return
        }

        prefs.apiEndpoint = trimmedEndpoint
        prefs.authType = s.authType
        prefs.basicUsername = s.basicUsername
        if (s.basicPassword.isNotBlank()) {
            prefs.basicPassword = s.basicPassword
        }
        if (s.bearerToken.isNotBlank()) {
            prefs.bearerToken = s.bearerToken
        }

        val validHeaders = s.customHeaders.filter { it.key.isNotBlank() }
        prefs.customHeaders = validHeaders

        app.setupPeriodicSyncWorker()
        _uiState.update {
            it.copy(
                customHeaders = if (validHeaders.isEmpty()) listOf(HeaderItem("X-API-Key", "")) else validHeaders,
                snackbarMessage = app.getString(com.sms2api.app.R.string.snack_config_saved)
            )
        }
    }

    fun updateAppLanguage(context: Context, languageTag: String) {
        prefs.appLanguage = languageTag
        _uiState.update { it.copy(appLanguage = languageTag) }
        LocaleHelper.applyLocale(context, languageTag)
    }

    fun showSnackbarMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
