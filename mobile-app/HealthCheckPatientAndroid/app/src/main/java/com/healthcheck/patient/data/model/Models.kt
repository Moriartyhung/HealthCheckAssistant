package com.healthcheck.patient.data.model

// 依 openapi.json components.schemas 建立。
// 欄位全部宣告為 nullable：openapi.json 未將這些 response 欄位標為 required。

/** ExamResponse */
data class ExamResponse(
    val examId: String? = null,
    val examName: String? = null,
    val stationId: String? = null,
    val stationName: String? = null,
    /** 例 "08:00" */
    val startTime: String? = null,
    /** 例 "11:00" */
    val endTime: String? = null,
    /** enum: PENDING, COMPLETED */
    val status: String? = null,
    /** date-time，Oracle TIMESTAMP 無時區，例 "2026-10-01T10:30:00"；未完成為 null */
    val completedAt: String? = null,
) {
    val isCompleted: Boolean get() = status == STATUS_COMPLETED

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}

/** PatientProgressResponse — GET /api/patients/{patientId}/exams */
data class PatientProgressResponse(
    val patientId: String? = null,
    val patientName: String? = null,
    val completedCount: Int? = null,
    val totalCount: Int? = null,
    val exams: List<ExamResponse>? = null,
)

/** NextExamResponse — GET /api/patients/{patientId}/next；nextExam 為 null 代表全部完成或目前無可進行項目 */
data class NextExamResponse(
    val nextExam: ExamResponse? = null,
    val message: String? = null,
)
