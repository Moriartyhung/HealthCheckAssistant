package com.healthcheckassistant.ws1;

import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.common.exam.ExamEligibility;
import com.healthcheckassistant.ws1.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class Ws1Service {
    private static final Logger log = LoggerFactory.getLogger(Ws1Service.class);
    private final Ws1Repository repository;
    private final Clock clock;

    public Ws1Service(Ws1Repository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Ws1CheckResponse check(Ws1CheckRequest request) {
        String patientId = request.patientId();
        String stationId = request.stationId();
        if (!repository.patientExists(patientId)) {
            throw new NotFoundException("找不到受檢者");
        }
        if (!repository.stationExists(stationId)) {
            throw new NotFoundException("找不到站點");
        }
        var exams = repository.findPatientExamAtStation(patientId, stationId);
        if (exams.size() > 1) {
            log.error("Multiple exams configured for patient {} at station {}", patientId, stationId);
            throw new IllegalStateException("Multiple exams configured for one patient/station");
        }
        var remaining = repository.findPendingExams(patientId);
        if (exams.isEmpty()) {
            return denied("受檢者未安排此站檢查", remaining);
        }
        var exam = exams.get(0);
        String denial = ExamEligibility.denial(exam.status(), exam.startTime(), exam.endTime(),
                LocalTime.now(clock),
                () -> repository.findIncompletePrerequisiteIds(patientId, exam.examId()).isEmpty());
        if (denial != null) {
            return denied(denial, remaining);
        }
        // Prototype v1 allows multiple unused confirmations for the same patient/exam.
        String code1 = UUID.randomUUID().toString();
        repository.insertConfirmation(code1, patientId, exam.examId());
        return new Ws1CheckResponse(true, "可以進行檢查", exam.examId(), exam.examName(),
                remaining.stream().filter(item -> !item.examId().equals(exam.examId())).toList(), code1);
    }

    private Ws1CheckResponse denied(String message, List<RemainingExamResponse> remaining) {
        return new Ws1CheckResponse(false, message, null, null, remaining, null);
    }
}

