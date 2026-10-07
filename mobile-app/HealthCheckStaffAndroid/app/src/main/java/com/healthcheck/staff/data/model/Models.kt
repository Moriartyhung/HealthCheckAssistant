package com.healthcheck.staff.data.model

// 依 openapi.json components.schemas 建立。

/** Ws1CheckRequest — required: patientId (max 20), stationId (max 10) */
data class Ws1CheckRequest(
    val patientId: String,
    val stationId: String,
)

/** RemainingExamResponse */
data class RemainingExamResponse(
    val examId: String? = null,
    val examName: String? = null,
)

/**
 * Ws1CheckResponse — 通過與正常業務拒絕均為 HTTP 200。
 * allowed=true 時 code1 為已寫入 Oracle 的 UUID 確認碼；拒絕時 code1 = null。
 */
data class Ws1CheckResponse(
    val allowed: Boolean? = null,
    val message: String? = null,
    val examId: String? = null,
    val examName: String? = null,
    val remainingExams: List<RemainingExamResponse>? = null,
    val code1: String? = null,
)

/** Ws2CompleteRequest — required: code1 (max 100) */
data class Ws2CompleteRequest(
    val code1: String,
)

/** Ws2CompleteResponse */
data class Ws2CompleteResponse(
    val success: Boolean? = null,
    val message: String? = null,
)
