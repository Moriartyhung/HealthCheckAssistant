package com.healthcheck.staff.data.api

import com.healthcheck.staff.data.model.Ws1CheckRequest
import com.healthcheck.staff.data.model.Ws1CheckResponse
import com.healthcheck.staff.data.model.Ws2CompleteRequest
import com.healthcheck.staff.data.model.Ws2CompleteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** 依 openapi.json；路徑不加前導 "/"，讓 Base URL 可含 context path。 */
interface HealthCheckApi {

    /** operationId: check — 200(allowed true/false) / 400 / 404 / 500 */
    @POST("api/ws1/check")
    suspend fun ws1Check(@Body body: Ws1CheckRequest): Response<Ws1CheckResponse>

    /** operationId: complete — 200 / 400 / 404 / 409 / 500 */
    @POST("api/ws2/complete")
    suspend fun ws2Complete(@Body body: Ws2CompleteRequest): Response<Ws2CompleteResponse>
}
