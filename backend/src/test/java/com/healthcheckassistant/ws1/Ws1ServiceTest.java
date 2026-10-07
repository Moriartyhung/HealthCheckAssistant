package com.healthcheckassistant.ws1;

import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.ws1.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class Ws1ServiceTest {
    private final Ws1Repository repository = mock(Ws1Repository.class);
    private static final List<RemainingExamResponse> PENDING = List.of(
        new RemainingExamResponse("E001", "血液檢查"), new RemainingExamResponse("E002", "胸部X光"), new RemainingExamResponse("E003", "心電圖"));

    private Ws1Service service(String time) {
        var instant = LocalDateTime.parse("2026-10-01T" + time).atZone(ZoneId.of("Asia/Taipei")).toInstant();
        return new Ws1Service(repository, Clock.fixed(instant, ZoneId.of("Asia/Taipei")));
    }
    private PatientExamAtStation exam(ExamStatus status) {
        return new PatientExamAtStation("E002", "胸部X光", status, LocalTime.of(8,0), LocalTime.of(16,0));
    }
    private void arrange(ExamStatus status) {
        when(repository.patientExists("P001")).thenReturn(true);
        when(repository.stationExists("S002")).thenReturn(true);
        when(repository.findPatientExamAtStation("P001", "S002")).thenReturn(List.of(exam(status)));
        when(repository.findPendingExams("P001")).thenReturn(PENDING);
        when(repository.findIncompletePrerequisiteIds("P001", "E002")).thenReturn(List.of());
    }
    private void neverInsert() { verify(repository, never()).insertConfirmation(anyString(), anyString(), anyString()); }

    @Test void allowedInsertsUuidAndExcludesOnlyCurrentExam() {
        arrange(ExamStatus.PENDING);
        var result = service("10:30:00").check(new Ws1CheckRequest("P001", "S002"));
        assertThat(result.allowed()).isTrue(); assertThat(result.message()).isEqualTo("可以進行檢查");
        assertThat(result.examId()).isEqualTo("E002"); assertThat(result.examName()).isEqualTo("胸部X光");
        assertThat(UUID.fromString(result.code1()).version()).isEqualTo(4);
        assertThat(result.remainingExams()).containsExactly(PENDING.get(0), PENDING.get(2));
        verify(repository).insertConfirmation(result.code1(), "P001", "E002");
        var order = inOrder(repository);
        order.verify(repository).patientExists("P001"); order.verify(repository).stationExists("S002");
        order.verify(repository).findPatientExamAtStation("P001", "S002");
        order.verify(repository).findPendingExams("P001");
        order.verify(repository).findIncompletePrerequisiteIds("P001", "E002");
        order.verify(repository).insertConfirmation(result.code1(), "P001", "E002");
    }
    @ParameterizedTest @CsvSource({"08:00:00,true", "16:00:00,true", "07:59:59,false", "16:00:01,false", "17:30:00,false"})
    void timeBoundariesAreInclusive(String time, boolean allowed) {
        arrange(ExamStatus.PENDING);
        var result = service(time).check(new Ws1CheckRequest("P001", "S002"));
        assertThat(result.allowed()).isEqualTo(allowed);
        if (allowed) verify(repository).insertConfirmation(result.code1(), "P001", "E002");
        else {
            assertThat(result.code1()).isNull(); assertThat(result.message()).isEqualTo("不在允許檢查時間（08:00–16:00）");
            assertThat(result.remainingExams()).isEqualTo(PENDING); neverInsert();
            verify(repository, never()).findIncompletePrerequisiteIds(anyString(), anyString());
        }
    }
    @Test void unknownPatientStopsBeforeStationLookup() {
        assertThatThrownBy(() -> service("10:30:00").check(new Ws1CheckRequest("P999", "S999")))
            .isInstanceOf(NotFoundException.class).hasMessage("找不到受檢者");
        verify(repository, never()).stationExists(anyString()); neverInsert();
    }
    @Test void unknownStationStopsBeforeExamLookup() {
        when(repository.patientExists("P001")).thenReturn(true);
        assertThatThrownBy(() -> service("10:30:00").check(new Ws1CheckRequest("P001", "S999")))
            .isInstanceOf(NotFoundException.class).hasMessage("找不到站點");
        verify(repository, never()).findPatientExamAtStation(anyString(), anyString()); neverInsert();
    }
    @Test void unassignedStationReturnsAllPendingExams() {
        arrange(ExamStatus.PENDING); when(repository.findPatientExamAtStation("P001", "S002")).thenReturn(List.of());
        var result = service("10:30:00").check(new Ws1CheckRequest("P001", "S002"));
        assertThat(result.allowed()).isFalse(); assertThat(result.message()).isEqualTo("受檢者未安排此站檢查");
        assertThat(result.remainingExams()).isEqualTo(PENDING); assertThat(result.code1()).isNull(); neverInsert();
    }
    @Test void completedPrecedesClosedTimeAndPrerequisites() {
        arrange(ExamStatus.COMPLETED);
        var result = service("17:30:00").check(new Ws1CheckRequest("P001", "S002"));
        assertThat(result.allowed()).isFalse(); assertThat(result.message()).isEqualTo("此檢查已完成，無需重複檢查");
        assertThat(result.code1()).isNull(); assertThat(result.remainingExams()).isEqualTo(PENDING); neverInsert();
        verify(repository, never()).findIncompletePrerequisiteIds(anyString(), anyString());
    }
    @Test void incompleteOrUnassignedPrerequisitesBlock() {
        arrange(ExamStatus.PENDING); when(repository.findIncompletePrerequisiteIds("P001", "E002")).thenReturn(List.of("E001"));
        var result = service("10:30:00").check(new Ws1CheckRequest("P001", "S002"));
        assertThat(result.allowed()).isFalse(); assertThat(result.message()).isEqualTo("前置檢查尚未完成");
        assertThat(result.code1()).isNull(); assertThat(result.remainingExams()).isEqualTo(PENDING); neverInsert();
    }
    @Test void allPrerequisitesCompletedAllowsExam() {
        arrange(ExamStatus.PENDING);
        when(repository.findPatientExamAtStation("P001", "S002")).thenReturn(List.of(new PatientExamAtStation("E005", "醫師總評", ExamStatus.PENDING, LocalTime.of(10,0), LocalTime.of(17,0))));
        when(repository.findIncompletePrerequisiteIds("P001", "E005")).thenReturn(List.of());
        var result = service("10:30:00").check(new Ws1CheckRequest("P001", "S002"));
        assertThat(result.allowed()).isTrue(); verify(repository).findIncompletePrerequisiteIds("P001", "E005");
        verify(repository).insertConfirmation(result.code1(), "P001", "E005");
    }
    @Test void multipleExamsAtStationAreConfigurationError() {
        arrange(ExamStatus.PENDING); when(repository.findPatientExamAtStation("P001", "S002")).thenReturn(List.of(exam(ExamStatus.PENDING), exam(ExamStatus.COMPLETED)));
        assertThatThrownBy(() -> service("10:30:00").check(new Ws1CheckRequest("P001", "S002"))).isInstanceOf(IllegalStateException.class);
        neverInsert();
    }
    @Test void repeatedChecksGenerateDistinctPersistedTokens() {
        arrange(ExamStatus.PENDING); var service = service("10:30:00");
        var first = service.check(new Ws1CheckRequest("P001", "S002")); var second = service.check(new Ws1CheckRequest("P001", "S002"));
        assertThat(first.code1()).isNotEqualTo(second.code1());
        verify(repository).insertConfirmation(first.code1(), "P001", "E002"); verify(repository).insertConfirmation(second.code1(), "P001", "E002");
    }
    @Test void failedInsertDoesNotReturnSuccess() {
        arrange(ExamStatus.PENDING); doThrow(new DataAccessResourceFailureException("DB failed")).when(repository).insertConfirmation(anyString(), anyString(), anyString());
        assertThatThrownBy(() -> service("10:30:00").check(new Ws1CheckRequest("P001", "S002"))).isInstanceOf(DataAccessResourceFailureException.class);
    }
}
