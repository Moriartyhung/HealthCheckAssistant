package com.healthcheckassistant.patient;

import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.patient.dto.PatientProgressResponse;
import org.springframework.stereotype.Service;

@Service
public class PatientService {
    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public PatientProgressResponse getExams(String patientId) {
        var patient = repository.findPatientById(patientId)
                .orElseThrow(() -> new NotFoundException("找不到受檢者"));
        var exams = repository.findExamsByPatientId(patientId);
        int completedCount = (int) exams.stream()
                .filter(exam -> exam.status() == ExamStatus.COMPLETED).count();
        return new PatientProgressResponse(patient.patientId(), patient.patientName(),
                completedCount, exams.size(), exams);
    }
}
