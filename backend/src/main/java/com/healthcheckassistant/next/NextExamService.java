package com.healthcheckassistant.next;

import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.common.exam.ExamEligibility;
import com.healthcheckassistant.next.dto.NextExamResponse;
import com.healthcheckassistant.patient.PatientRepository;
import com.healthcheckassistant.patient.dto.ExamResponse;
import com.healthcheckassistant.patient.dto.ExamStatus;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalTime;
import java.util.Comparator;

@Service
public class NextExamService {
    private final PatientRepository repository;
    private final Clock clock;

    public NextExamService(PatientRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public NextExamResponse getNext(String patientId) {
        repository.findPatientById(patientId).orElseThrow(() -> new NotFoundException("找不到受檢者"));
        var pending = repository.findExamsByPatientId(patientId).stream()
                .filter(exam -> exam.status() == ExamStatus.PENDING)
                .sorted(Comparator.comparing(ExamResponse::examId)).toList();
        if (pending.isEmpty()) {
            return new NextExamResponse(null, "所有檢查已完成，謝謝您的配合！");
        }
        var now = LocalTime.now(clock);
        for (var exam : pending) {
            String denial = ExamEligibility.denial(exam.status(),
                    ExamEligibility.parseTime(exam.startTime()), ExamEligibility.parseTime(exam.endTime()),
                    now, () -> repository.findIncompletePrerequisiteIds(patientId, exam.examId()).isEmpty());
            if (denial == null) {
                return new NextExamResponse(exam, "建議前往以下站點");
            }
        }
        return new NextExamResponse(null, "目前沒有可進行的檢查，請洽工作人員");
    }
}
