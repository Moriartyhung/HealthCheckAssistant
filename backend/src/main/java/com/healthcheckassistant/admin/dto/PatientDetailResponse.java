package com.healthcheckassistant.admin.dto;
import java.util.List;
public record PatientDetailResponse(String patientId, String patientName, List<ManagedPatientExam> exams) {}
