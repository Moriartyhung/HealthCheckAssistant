package com.healthcheckassistant.ws1;

import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.ws1.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Exercises the real @Transactional service proxy without connecting to Oracle.
@SpringBootTest(properties="DB_PASSWORD=unused-test-placeholder")
@Import(Ws1TransactionTest.FixedClockConfig.class)
class Ws1TransactionTest {
    @TestConfiguration static class FixedClockConfig {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-01T02:30:00Z"), ZoneId.of("Asia/Taipei"));
        }
    }
    @Autowired Ws1Service service;
    @MockitoBean Ws1Repository repository;
    @MockitoBean PlatformTransactionManager transactionManager;
    @BeforeEach void arrange() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        when(repository.patientExists("P001")).thenReturn(true);
        when(repository.stationExists("S002")).thenReturn(true);
        when(repository.findPatientExamAtStation("P001","S002")).thenReturn(List.of(new PatientExamAtStation("E002","胸部X光",ExamStatus.PENDING,LocalTime.of(8,0),LocalTime.of(16,0))));
        when(repository.findPendingExams("P001")).thenReturn(List.of(new RemainingExamResponse("E002","胸部X光")));
        when(repository.findIncompletePrerequisiteIds("P001","E002")).thenReturn(List.of());
    }
    @Test void successfulCheckCommitsAfterConfirmationWrite() {
        var response=service.check(new Ws1CheckRequest("P001","S002"));
        assertThat(response.allowed()).isTrue(); assertThat(response.remainingExams()).isEmpty();
        var order=inOrder(transactionManager,repository);
        order.verify(transactionManager).getTransaction(any());
        order.verify(repository).insertConfirmation(response.code1(),"P001","E002");
        order.verify(transactionManager).commit(any());
        verify(transactionManager,never()).rollback(any());
    }
    @Test void failedConfirmationWriteRollsBack() {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("insert failed")).when(repository).insertConfirmation(anyString(),anyString(),anyString());
        assertThatThrownBy(() -> service.check(new Ws1CheckRequest("P001","S002"))).isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
        verify(transactionManager).rollback(any()); verify(transactionManager,never()).commit(any());
    }
}
