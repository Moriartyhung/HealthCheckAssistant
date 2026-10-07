package com.healthcheckassistant.ws2;

import java.time.LocalDateTime;

public record ConfirmationData(String code1, String patientId, String examId,
                               LocalDateTime createdAt, LocalDateTime usedAt) {}
