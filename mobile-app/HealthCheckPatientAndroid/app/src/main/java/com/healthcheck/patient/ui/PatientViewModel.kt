package com.healthcheck.patient.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthcheck.patient.data.api.ApiConfig
import com.healthcheck.patient.data.model.NextExamResponse
import com.healthcheck.patient.data.model.PatientProgressResponse
import com.healthcheck.patient.data.repository.ApiResult
import com.healthcheck.patient.data.repository.PatientRepository
import com.healthcheck.patient.data.repository.errorText
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PatientUiState {
    data object Idle : PatientUiState
    data object Loading : PatientUiState
    data class Success(
        val progress: PatientProgressResponse,
        /** next API 結果；成功時為 response，失敗時為 null 並以 nextError 說明 */
        val next: NextExamResponse?,
        val nextError: String?,
        val refreshing: Boolean = false,
    ) : PatientUiState
    data class Error(val message: String) : PatientUiState
}

class PatientViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PatientRepository(ApiConfig.getBaseUrl(application))

    private val _patientId = MutableStateFlow<String?>(null)
    val patientId: StateFlow<String?> = _patientId.asStateFlow()

    private val _uiState = MutableStateFlow<PatientUiState>(PatientUiState.Idle)
    val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

    private val _baseUrl = MutableStateFlow(repository.baseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private var loadJob: Job? = null

    /** 回傳錯誤訊息；null 表示通過。限制依 openapi.json：patientId maxLength 20 */
    fun validatePatientId(raw: String): String? {
        val id = raw.trim()
        return when {
            id.isEmpty() -> "請輸入 Patient ID"
            id.length > 20 -> "Patient ID 最多 20 字元"
            else -> null
        }
    }

    fun query(raw: String) {
        val id = raw.trim()
        if (validatePatientId(id) != null) return
        _patientId.value = id
        load(id, isRefresh = false)
    }

    fun refresh() {
        val id = _patientId.value ?: return
        load(id, isRefresh = true)
    }

    fun backToInput() {
        loadJob?.cancel()
        _patientId.value = null
        _uiState.value = PatientUiState.Idle
    }

    fun updateBaseUrl(raw: String): String? {
        val url = ApiConfig.normalize(raw) ?: return "網址格式錯誤，請輸入包含有效主機的 http:// 或 https:// 網址"
        try {
            repository.updateBaseUrl(url)
        } catch (_: IllegalArgumentException) {
            return "網址無法作為 API 位址，請確認主機、連接埠與路徑"
        }
        ApiConfig.setBaseUrl(getApplication(), url)
        _baseUrl.value = url
        return null
    }

    fun resetBaseUrl() {
        val url = ApiConfig.defaultBaseUrl()
        repository.updateBaseUrl(url)
        ApiConfig.resetBaseUrl(getApplication())
        _baseUrl.value = url
    }

    private fun load(id: String, isRefresh: Boolean) {
        loadJob?.cancel()
        val current = _uiState.value
        if (isRefresh && current is PatientUiState.Success) {
            _uiState.update { current.copy(refreshing = true) }
        } else {
            _uiState.value = PatientUiState.Loading
        }
        loadJob = viewModelScope.launch {
            val examsDeferred = async { repository.getExams(id) }
            val nextDeferred = async { repository.getNext(id) }
            val exams = examsDeferred.await()
            val next = nextDeferred.await()
            _uiState.value = when (exams) {
                is ApiResult.Success -> PatientUiState.Success(
                    progress = exams.data,
                    next = (next as? ApiResult.Success)?.data,
                    nextError = if (next is ApiResult.Success) null else next.errorText(),
                )
                else -> PatientUiState.Error(exams.errorText())
            }
        }
    }
}
