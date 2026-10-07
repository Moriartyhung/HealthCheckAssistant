package com.healthcheckassistant.ws2;

import com.healthcheckassistant.common.error.ConflictException;
import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.ws2.dto.Ws2CompleteRequest;
import com.healthcheckassistant.ws2.dto.Ws2CompleteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Ws2Service {
    private final Ws2Repository repository;

    public Ws2Service(Ws2Repository repository) {
        this.repository = repository;
    }

    @Transactional
    public Ws2CompleteResponse complete(Ws2CompleteRequest request) {
        var confirmation = repository.findConfirmation(request.code1())
                .orElseThrow(() -> new NotFoundException("無效的確認碼，請重新判斷"));
        if (confirmation.usedAt() != null) {
            throw new ConflictException("確認碼已使用，請勿重複提交");
        }
        var exam = repository.findPatientExam(confirmation.patientId(), confirmation.examId())
                .orElseThrow(() -> new IllegalStateException("Confirmation references a missing patient exam"));
        if (exam.status() == ExamStatus.COMPLETED) {
            throw new ConflictException("此檢查已完成");
        }
        int claimed = repository.markConfirmationUsed(request.code1());
        if (claimed == 0) {
            throw new ConflictException("確認碼已使用，請勿重複提交");
        }
        if (claimed != 1) {
            throw new IllegalStateException("Confirmation claim did not update exactly one row");
        }
        int completed = repository.completeExam(confirmation.patientId(), confirmation.examId());
        if (completed == 0) {
            // Runtime conflict rolls back this request's claim if another token won the exam race.
            throw new ConflictException("此檢查已完成");
        }
        if (completed != 1) {
            throw new IllegalStateException("Exam completion did not update exactly one row");
        }
        return new Ws2CompleteResponse(true, "檢查完成狀態已更新");
    }
}
