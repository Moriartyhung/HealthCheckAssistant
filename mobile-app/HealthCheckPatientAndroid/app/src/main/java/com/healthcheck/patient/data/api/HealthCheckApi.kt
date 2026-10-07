package com.healthcheck.patient.data.api

import com.healthcheck.patient.data.model.NextExamResponse
import com.healthcheck.patient.data.model.PatientProgressResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** 依 openapi.json；路徑不加前導 "/"，讓 Base URL 可含 context path。 */
interface HealthCheckApi {

    /** operationId: getExams — 200 / 400 / 404 / 500 */
    @GET("api/patients/{patientId}/exams")
    suspend fun getExams(@Path("patientId") patientId: String): Response<PatientProgressResponse>

    /** operationId: getNext — 200 / 400 / 404 / 500 */
    @GET("api/patients/{patientId}/next")
    suspend fun getNext(@Path("patientId") patientId: String): Response<NextExamResponse>
}
