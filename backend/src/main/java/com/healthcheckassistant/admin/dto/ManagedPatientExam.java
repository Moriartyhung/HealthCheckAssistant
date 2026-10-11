package com.healthcheckassistant.admin.dto;
import com.healthcheckassistant.patient.dto.ExamStatus;
import java.time.LocalDateTime;
public record ManagedPatientExam(String examId, String examName, String stationId, String stationName,
                                 String startTime, String endTime, ExamStatus status, LocalDateTime completedAt,
                                 boolean removable, String removalBlockReason) {}
