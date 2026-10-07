package com.healthcheck.staff.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthcheck.staff.data.api.ApiConfig
import com.healthcheck.staff.data.model.Ws1CheckResponse
import com.healthcheck.staff.data.model.Ws2CompleteResponse
import com.healthcheck.staff.data.repository.ApiResult
import com.healthcheck.staff.data.repository.StaffRepository
import com.healthcheck.staff.data.repository.errorText
import com.healthcheck.staff.scanner.QRScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val PATIENT_ID_MAX = 20   // openapi.json Ws1CheckRequest.patientId maxLength
const val STATION_ID_MAX = 10   // openapi.json Ws1CheckRequest.stationId maxLength

enum class ScanTarget { PATIENT, STATION }

/** WS1 進站判斷狀態 */
sealed interface Ws1State {
    data object Idle : Ws1State
    data object Loading : Ws1State
    /** HTTP 200：allowed=true 或 allowed=false（業務拒絕，屬正常結果） */
    data class Result(val patientId: String, val stationId: String, val response: Ws1CheckResponse) : Ws1State
    /** 400 / 404 / 500 / 網路 / 解析錯誤 */
    data class Error(val message: String) : Ws1State
}

/** WS2 完成檢查狀態 */
sealed interface Ws2State {
    data object Idle : Ws2State
    data object Loading : Ws2State
    data class Result(val response: Ws2CompleteResponse) : Ws2State
    data class Error(val code: Int?, val message: String) : Ws2State
}

data class StaffUiState(
    val patientId: String = "",
    val stationId: String = "",
    val patientIdError: String? = null,
    val stationIdError: String? = null,
    val ws1: Ws1State = Ws1State.Idle,
    /** WS1 allowed=true 時保存，WS2 成功或使用者重設後清除 */
    val code1: String? = null,
    val ws2: Ws2State = Ws2State.Idle,
    val scanTarget: ScanTarget? = null,
    val scanMessage: String? = null,
) {
    val busy: Boolean get() = ws1 is Ws1State.Loading || ws2 is Ws2State.Loading
}

class StaffViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StaffRepository(ApiConfig.getBaseUrl(application))

    private val _state = MutableStateFlow(StaffUiState())
    val state: StateFlow<StaffUiState> = _state.asStateFlow()

    private val _baseUrl = MutableStateFlow(repository.baseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    // ---------- 輸入 ----------

    fun onPatientIdChange(v: String) = _state.update {
        if (it.busy) it else it.copy(patientId = v, patientIdError = null).clearedResults()
    }

    fun onStationIdChange(v: String) = _state.update {
        if (it.busy) it else it.copy(stationId = v, stationIdError = null).clearedResults()
    }

    // ---------- QR ----------

    fun startScan(target: ScanTarget) = _state.update {
        if (it.busy) it else it.copy(scanTarget = target, scanMessage = null)
    }

    fun cancelScan() = _state.update { it.copy(scanTarget = null) }

    /** @return true 表示已接受並關閉掃描畫面；false 表示內容無效，繼續掃描 */
    fun onQrScanned(raw: String): Boolean {
        if (_state.value.busy) return false
        val target = _state.value.scanTarget ?: return false
        val max = if (target == ScanTarget.PATIENT) PATIENT_ID_MAX else STATION_ID_MAX
        val err = QRScanner.validate(raw, max, if (target == ScanTarget.PATIENT) 'P' else 'S')
        if (err != null) {
            _state.update { it.copy(scanMessage = err) }
            return false
        }
        val value = raw.trim()
        _state.update {
            when (target) {
                ScanTarget.PATIENT -> it.copy(patientId = value, patientIdError = null)
                ScanTarget.STATION -> it.copy(stationId = value, stationIdError = null)
            }.clearedResults().copy(scanTarget = null, scanMessage = null)
        }
        return true
    }

    fun onScanMessage(msg: String) = _state.update { it.copy(scanMessage = msg) }

    // ---------- WS1 ----------

    fun checkEligibility() {
        val s = _state.value
        if (s.busy) return
        val pid = s.patientId.trim()
        val sid = s.stationId.trim()
        val pErr = when {
            pid.isEmpty() -> "請輸入或掃描 Patient ID"
            pid.length > PATIENT_ID_MAX -> "Patient ID 最多 $PATIENT_ID_MAX 字元"
            else -> null
        }
        val sErr = when {
            sid.isEmpty() -> "請輸入或掃描 Station ID"
            sid.length > STATION_ID_MAX -> "Station ID 最多 $STATION_ID_MAX 字元"
            else -> null
        }
        if (pErr != null || sErr != null) {
            _state.update { it.copy(patientIdError = pErr, stationIdError = sErr) }
            return
        }
        _state.update {
            it.copy(patientId = pid, stationId = sid, ws1 = Ws1State.Loading, code1 = null, ws2 = Ws2State.Idle)
        }
        viewModelScope.launch {
            val result = repository.ws1Check(pid, sid)
            _state.update {
                when (result) {
                    is ApiResult.Success -> {
                        val r = result.data
                        it.copy(
                            ws1 = Ws1State.Result(pid, sid, r),
                            // 只有 allowed=true 且有 code1 才保存
                            code1 = if (r.allowed == true) r.code1?.takeIf { c -> c.isNotBlank() } else null,
                        )
                    }
                    else -> it.copy(ws1 = Ws1State.Error(result.errorText()), code1 = null)
                }
            }
        }
    }

    // ---------- WS2 ----------

    fun confirmComplete() {
        val s = _state.value
        val code = s.code1 ?: return
        if (s.busy) return
        _state.update { it.copy(ws2 = Ws2State.Loading) }
        viewModelScope.launch {
            val result = repository.ws2Complete(code)
            _state.update {
                when (result) {
                    is ApiResult.Success -> {
                        val ok = result.data.success == true
                        it.copy(
                            ws2 = Ws2State.Result(result.data),
                            // 成功後確認碼已被 Backend 消耗，清除避免重送
                            code1 = if (ok) null else it.code1,
                        )
                    }
                    is ApiResult.HttpError -> it.copy(
                        ws2 = Ws2State.Error(result.code, result.errorText()),
                        // 404 無效 / 409 已使用：此 code1 不可能再成功，清除
                        code1 = if (result.code == 404 || result.code == 409) null else it.code1,
                    )
                    else -> it.copy(ws2 = Ws2State.Error(null, result.errorText()))
                }
            }
        }
    }

    /** 下一位受檢者：清除所有輸入與結果 */
    fun resetAll() = _state.update { if (it.busy) it else StaffUiState() }

    // ---------- 伺服器設定 ----------

    fun updateBaseUrl(raw: String): String? {
        if (_state.value.busy) return "請等待目前的資格檢查或完成送出結束，再修改伺服器"
        val url = ApiConfig.normalize(raw) ?: return "網址格式錯誤，請輸入包含有效主機的 http:// 或 https:// 網址"
        try {
            repository.updateBaseUrl(url)
        } catch (_: IllegalArgumentException) {
            return "網址無法作為 API 位址，請確認主機、連接埠與路徑"
        }
        if (url != _baseUrl.value) _state.update { it.clearedResults() }
        ApiConfig.setBaseUrl(getApplication(), url)
        _baseUrl.value = url
        return null
    }

    fun resetBaseUrl() {
        if (_state.value.busy) return
        val url = ApiConfig.defaultBaseUrl()
        repository.updateBaseUrl(url)
        if (url != _baseUrl.value) _state.update { it.clearedResults() }
        ApiConfig.resetBaseUrl(getApplication())
        _baseUrl.value = url
    }

    private fun StaffUiState.clearedResults(): StaffUiState =
        if (busy) this
        else copy(ws1 = Ws1State.Idle, code1 = null, ws2 = Ws2State.Idle)
}
