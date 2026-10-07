package com.healthcheckassistant.patient.dto;

import java.util.List;

public record PatientProgressResponse(
        String patientId, String patientName, int completedCount, int totalCount,
        List<ExamResponse> exams) {}
