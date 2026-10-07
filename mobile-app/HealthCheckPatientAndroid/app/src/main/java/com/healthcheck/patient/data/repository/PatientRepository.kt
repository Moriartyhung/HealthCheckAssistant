package com.healthcheck.patient.data.repository

import com.healthcheck.patient.data.api.ApiConfig
import com.healthcheck.patient.data.api.HealthCheckApi
import com.healthcheck.patient.data.model.NextExamResponse
import com.healthcheck.patient.data.model.PatientProgressResponse

class PatientRepository(baseUrl: String) {

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

    suspend fun getExams(patientId: String): ApiResult<PatientProgressResponse> =
        safeApiCall(
            mapOf(400 to "參數不正確", 404 to "找不到受檢者", 500 to "服務暫時無法使用")
        ) { api.getExams(patientId) }

    suspend fun getNext(patientId: String): ApiResult<NextExamResponse> =
        safeApiCall(
            mapOf(400 to "參數不正確", 404 to "找不到受檢者", 500 to "服務暫時無法使用")
        ) { api.getNext(patientId) }
}
