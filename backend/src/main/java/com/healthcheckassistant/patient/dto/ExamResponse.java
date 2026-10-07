package com.healthcheckassistant.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ExamResponse(
        String examId,
        String examName,
        String stationId,
        String stationName,
        @Schema(example = "08:00") String startTime,
        @Schema(example = "11:00") String endTime,
        ExamStatus status,
        @Schema(description = "完成時間；Oracle TIMESTAMP 無時區，未完成為 null", nullable = true,
                example = "2026-10-01T10:30:00") LocalDateTime completedAt) {}
