package com.healthcheckassistant.ws2;

import com.healthcheckassistant.common.error.ConflictException;
import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.ws2.dto.Ws2CompleteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real Spring service proxy; mock transaction manager/repository keep ordinary tests off Oracle.
@SpringBootTest(properties="DB_PASSWORD=unused-test-placeholder")
@AutoConfigureMockMvc
class Ws2TransactionTest {
    @Autowired Ws2Service service;
    @Autowired MockMvc mvc;
    @MockitoBean Ws2Repository repository;
    @MockitoBean PlatformTransactionManager transactionManager;
    @BeforeEach void arrange() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        when(repository.findConfirmation("codeA")).thenReturn(Optional.of(new ConfirmationData("codeA","P001","E002",LocalDateTime.of(2026,10,1,10,30),null)));
        when(repository.findPatientExam("P001","E002")).thenReturn(Optional.of(new PatientExamCompletionData(ExamStatus.PENDING,null)));
        when(repository.markConfirmationUsed("codeA")).thenReturn(1);
        when(repository.completeExam("P001","E002")).thenReturn(1);
    }
    @Test void successCommitsOnlyAfterBothUpdates() {
        assertThat(service.complete(new Ws2CompleteRequest("codeA")).success()).isTrue();
        var order=inOrder(transactionManager,repository);
        order.verify(transactionManager).getTransaction(any());
        order.verify(repository).markConfirmationUsed("codeA"); order.verify(repository).completeExam("P001","E002");
        order.verify(transactionManager).commit(any()); verify(transactionManager,never()).rollback(any());
    }
    @Test void examRaceRollsBackClaimRatherThanCommitting() {
        when(repository.completeExam("P001","E002")).thenReturn(0);
        assertThatThrownBy(() -> service.complete(new Ws2CompleteRequest("codeA"))).isInstanceOf(ConflictException.class).hasMessage("此檢查已完成");
        var order=inOrder(repository,transactionManager);
        order.verify(repository).markConfirmationUsed("codeA"); order.verify(repository).completeExam("P001","E002");
        order.verify(transactionManager).rollback(any()); verify(transactionManager,never()).commit(any());
    }
    @Test void lostClaimRollsBackBeforeControllerReturns409() throws Exception {
        when(repository.markConfirmationUsed("codeA")).thenReturn(0);
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content("{\"code1\":\"codeA\"}")).andExpect(status().isConflict()).andExpect(content().json("{\"message\":\"確認碼已使用，請勿重複提交\"}",true));
        verify(transactionManager).rollback(any()); verify(transactionManager,never()).commit(any());
        verify(repository,never()).completeExam(anyString(),anyString());
    }
    @Test void secondUpdateDatabaseErrorRollsBackBeforeSafe500() throws Exception {
        when(repository.completeExam("P001","E002")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Oracle SQL internals"));
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content("{\"code1\":\"codeA\"}")).andExpect(status().isInternalServerError()).andExpect(content().json("{\"message\":\"服務暫時無法使用\"}",true));
        verify(repository).markConfirmationUsed("codeA"); verify(transactionManager).rollback(any()); verify(transactionManager,never()).commit(any());
    }
}
