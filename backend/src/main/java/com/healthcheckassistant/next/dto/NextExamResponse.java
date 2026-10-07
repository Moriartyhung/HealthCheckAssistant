package com.healthcheckassistant.next.dto;

import com.healthcheckassistant.patient.dto.ExamResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record NextExamResponse(
        @Schema(description = "下一項可進行的檢查；全部完成或目前無可進行項目時為 null", nullable = true)
        ExamResponse nextExam,
        String message) {}
