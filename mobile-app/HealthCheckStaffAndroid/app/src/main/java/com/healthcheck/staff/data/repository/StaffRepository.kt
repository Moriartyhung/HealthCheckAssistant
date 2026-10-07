package com.healthcheck.staff.data.repository

import com.healthcheck.staff.data.api.ApiConfig
import com.healthcheck.staff.data.api.HealthCheckApi
import com.healthcheck.staff.data.model.Ws1CheckRequest
import com.healthcheck.staff.data.model.Ws1CheckResponse
import com.healthcheck.staff.data.model.Ws2CompleteRequest
import com.healthcheck.staff.data.model.Ws2CompleteResponse

class StaffRepository(baseUrl: String) {

    private var currentBaseUrl: String = baseUrl
    private var api: HealthCheckApi = ApiConfig.createApi(baseUrl)

    val baseUrl: String get() = currentBaseUrl

    fun updateBaseUrl(url: String) {
        if (url != currentBaseUrl) {
            val newApi = ApiConfig.createApi(url)
            api = newApi
            currentBaseUrl = url
        }
    }

    // 預設訊息取自 openapi.json 各 response description
    suspend fun ws1Check(patientId: String, stationId: String): ApiResult<Ws1CheckResponse> =
        safeApiCall(
            mapOf(400 to "請求格式不正確", 404 to "找不到受檢者或站點", 500 to "伺服器處理失敗")
        ) { api.ws1Check(Ws1CheckRequest(patientId, stationId)) }

    suspend fun ws2Complete(code1: String): ApiResult<Ws2CompleteResponse> =
        safeApiCall(
            mapOf(400 to "請提供 code1", 404 to "無效的確認碼", 409 to "確認碼已使用或此檢查已完成", 500 to "服務暫時無法使用")
        ) { api.ws2Complete(Ws2CompleteRequest(code1)) }
}
