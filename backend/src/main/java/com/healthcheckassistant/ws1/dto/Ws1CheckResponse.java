package com.healthcheckassistant.ws1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record Ws1CheckResponse(
        boolean allowed,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String examId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String examName,
        List<RemainingExamResponse> remainingExams,
        @Schema(description = "允許時為已寫入 Oracle 的 UUID 確認碼；拒絕時為 null", nullable = true)
        String code1) {}
