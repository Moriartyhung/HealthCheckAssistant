package com.healthcheckassistant.ws2;

import com.healthcheckassistant.common.error.*;
import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.ws2.dto.Ws2CompleteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class Ws2ServiceTest {
    private final Ws2Repository repository = mock(Ws2Repository.class);
    private final Ws2Service service = new Ws2Service(repository);
    private final Ws2CompleteRequest request = new Ws2CompleteRequest("codeA");
    private void arrange() {
        // Old confirmation is still valid: Prototype v1 has no expiry.
        when(repository.findConfirmation("codeA")).thenReturn(Optional.of(new ConfirmationData("codeA","P001","E002",LocalDateTime.of(2000,1,1,0,0),null)));
        when(repository.findPatientExam("P001","E002")).thenReturn(Optional.of(new PatientExamCompletionData(ExamStatus.PENDING,null)));
        when(repository.markConfirmationUsed("codeA")).thenReturn(1);
        when(repository.completeExam("P001","E002")).thenReturn(1);
    }
    private void neverUpdate() {
        verify(repository,never()).markConfirmationUsed(anyString());
        verify(repository,never()).completeExam(anyString(),anyString());
    }
    @Test void successClaimsTokenThenCompletesExam() {
        arrange(); var result=service.complete(request);
        assertThat(result.success()).isTrue(); assertThat(result.message()).isEqualTo("檢查完成狀態已更新");
        var order=inOrder(repository);
        order.verify(repository).findConfirmation("codeA"); order.verify(repository).findPatientExam("P001","E002");
        order.verify(repository).markConfirmationUsed("codeA"); order.verify(repository).completeExam("P001","E002");
        verifyNoMoreInteractions(repository);
    }
    @Test void unknownTokenDoesNotQueryExamOrUpdate() {
        when(repository.findConfirmation("codeA")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(NotFoundException.class).hasMessage("無效的確認碼，請重新判斷");
        verify(repository,never()).findPatientExam(anyString(),anyString()); neverUpdate();
    }
    @Test void usedTokenPrecedesExamState() {
        arrange(); when(repository.findConfirmation("codeA")).thenReturn(Optional.of(new ConfirmationData("codeA","P001","E002",LocalDateTime.now(),LocalDateTime.now())));
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(ConflictException.class).hasMessage("確認碼已使用，請勿重複提交");
        verify(repository,never()).findPatientExam(anyString(),anyString()); neverUpdate();
    }
    @Test void completedExamDoesNotConsumeUnusedSecondToken() {
        arrange(); when(repository.findPatientExam("P001","E002")).thenReturn(Optional.of(new PatientExamCompletionData(ExamStatus.COMPLETED,LocalDateTime.now())));
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(ConflictException.class).hasMessage("此檢查已完成"); neverUpdate();
    }
    @Test void missingReferencedExamIsConfigurationError() {
        arrange(); when(repository.findPatientExam("P001","E002")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(IllegalStateException.class); neverUpdate();
    }
    @Test void lostTokenClaimDoesNotUpdateExam() {
        arrange(); when(repository.markConfirmationUsed("codeA")).thenReturn(0);
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(ConflictException.class).hasMessage("確認碼已使用，請勿重複提交");
        verify(repository,never()).completeExam(anyString(),anyString());
    }
    @Test void lostExamRaceThrowsAfterClaimForTransactionRollback() {
        arrange(); when(repository.completeExam("P001","E002")).thenReturn(0);
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(ConflictException.class).hasMessage("此檢查已完成");
        verify(repository).markConfirmationUsed("codeA");
    }
    @Test void claimDatabaseFailureIsNotSwallowed() {
        arrange(); when(repository.markConfirmationUsed("codeA")).thenThrow(new DataAccessResourceFailureException("DB unavailable"));
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(DataAccessResourceFailureException.class);
        verify(repository,never()).completeExam(anyString(),anyString());
    }
    @Test void examDatabaseFailureIsNotSwallowedAfterClaim() {
        arrange(); when(repository.completeExam("P001","E002")).thenThrow(new DataAccessResourceFailureException("DB failed"));
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(DataAccessResourceFailureException.class);
        verify(repository).markConfirmationUsed("codeA");
    }
    @Test void unexpectedMultipleAffectedRowsFailsInsteadOfSuccess() {
        arrange(); when(repository.markConfirmationUsed("codeA")).thenReturn(2);
        assertThatThrownBy(() -> service.complete(request)).isInstanceOf(IllegalStateException.class);
        verify(repository,never()).completeExam(anyString(),anyString());
    }
}
