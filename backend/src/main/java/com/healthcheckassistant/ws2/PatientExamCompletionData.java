package com.healthcheckassistant.ws2;

import com.healthcheckassistant.patient.dto.ExamStatus;
import java.time.LocalDateTime;

public record PatientExamCompletionData(ExamStatus status, LocalDateTime completedAt) {}
