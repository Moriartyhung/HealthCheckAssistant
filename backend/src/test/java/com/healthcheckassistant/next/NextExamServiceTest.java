package com.healthcheckassistant.next;

import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.patient.*;
import com.healthcheckassistant.patient.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NextExamServiceTest {
    PatientRepository repository = mock(PatientRepository.class);
    @BeforeEach void knownPatient() {
        when(repository.findPatientById("P001")).thenReturn(Optional.of(new PatientBasicData("P001", "受檢者")));
    }
    NextExamService service(String time) {
        return new NextExamService(repository, Clock.fixed(
                ZonedDateTime.of(LocalDate.of(2026,10,1), LocalTime.parse(time),
                        ZoneId.of("Asia/Taipei")).toInstant(), ZoneId.of("Asia/Taipei")));
    }
    ExamResponse exam(String id, ExamStatus status, String start, String end) {
        return new ExamResponse(id, "檢查"+id, "S001", "站點", start, end, status,
                status == ExamStatus.COMPLETED ? LocalDateTime.of(2026,10,1,9,0) : null);
    }
    void exams(ExamResponse... exams) {
        when(repository.findExamsByPatientId("P001")).thenReturn(List.of(exams));
    }
    @Test void unknownPatientStopsBeforeExamQueries() {
        assertThatThrownBy(() -> service("10:30").getNext("P999"))
                .isInstanceOf(NotFoundException.class).hasMessage("找不到受檢者");
        verify(repository, never()).findExamsByPatientId(anyString());
    }
    @Test void choosesFirstByExamIdEvenIfInputIsUnsorted() {
        var first = exam("E001", ExamStatus.PENDING, "08:00", "11:00");
        exams(exam("E002", ExamStatus.PENDING, "08:00", "16:00"), first);
        var result = service("10:30").getNext("P001");
        assertThat(result.nextExam()).isEqualTo(first);
        assertThat(result.nextExam().completedAt()).isNull();
        assertThat(result.message()).isEqualTo("建議前往以下站點");
        verify(repository, never()).findIncompletePrerequisiteIds("P001", "E002");
    }
    @Test void skipsCompleted() {
        exams(exam("E001", ExamStatus.COMPLETED, "08:00", "11:00"),
                exam("E002", ExamStatus.PENDING, "08:00", "16:00"));
        assertThat(service("10:30").getNext("P001").nextExam().examId()).isEqualTo("E002");
        verify(repository, never()).findIncompletePrerequisiteIds("P001", "E001");
    }
    @Test void skipsClosedWindow() {
        exams(exam("E001", ExamStatus.PENDING, "08:00", "11:00"),
                exam("E002", ExamStatus.PENDING, "08:00", "16:00"));
        assertThat(service("12:00").getNext("P001").nextExam().examId()).isEqualTo("E002");
        verify(repository, never()).findIncompletePrerequisiteIds("P001", "E001");
    }
    @Test void includesStartBoundary() {
        exams(exam("E001", ExamStatus.PENDING, "08:00", "11:00"));
        assertThat(service("08:00").getNext("P001").nextExam()).isNotNull();
    }
    @Test void includesEndBoundary() {
        exams(exam("E001", ExamStatus.PENDING, "08:00", "11:00"));
        assertThat(service("11:00").getNext("P001").nextExam()).isNotNull();
    }
    @Test void beforeStartIsUnavailable() {
        exams(exam("E001", ExamStatus.PENDING, "08:00", "11:00"));
        assertThat(service("07:59:59").getNext("P001").nextExam()).isNull();
    }
    @Test void afterEndByOneSecondIsUnavailable() {
        exams(exam("E001", ExamStatus.PENDING, "08:00", "11:00"));
        assertThat(service("11:00:01").getNext("P001").nextExam()).isNull();
    }
    @Test void blockedPrerequisiteIsUnavailableIncludingMissingAssignment() {
        exams(exam("E005", ExamStatus.PENDING, "10:00", "17:00"));
        when(repository.findIncompletePrerequisiteIds("P001", "E005")).thenReturn(List.of("E004"));
        var result = service("10:30").getNext("P001");
        assertThat(result.nextExam()).isNull();
        assertThat(result.message()).isEqualTo("目前沒有可進行的檢查，請洽工作人員");
    }
    @Test void skipsBlockedCandidateToNextEligible() {
        exams(exam("E001", ExamStatus.PENDING, "08:00", "16:00"),
                exam("E002", ExamStatus.PENDING, "08:00", "16:00"));
        when(repository.findIncompletePrerequisiteIds("P001", "E001")).thenReturn(List.of("E003"));
        assertThat(service("10:30").getNext("P001").nextExam().examId()).isEqualTo("E002");
    }
    @Test void completedPrerequisitesPermitExam() {
        exams(exam("E001", ExamStatus.COMPLETED, "08:00", "11:00"),
                exam("E002", ExamStatus.COMPLETED, "08:00", "16:00"),
                exam("E003", ExamStatus.COMPLETED, "08:00", "15:00"),
                exam("E004", ExamStatus.COMPLETED, "09:00", "15:00"),
                exam("E005", ExamStatus.PENDING, "10:00", "17:00"));
        when(repository.findIncompletePrerequisiteIds("P001", "E005")).thenReturn(List.of());
        assertThat(service("10:30").getNext("P001").nextExam().examId()).isEqualTo("E005");
    }
    @Test void allCompleted() {
        exams(exam("E001", ExamStatus.COMPLETED, "08:00", "11:00"));
        var result = service("23:00").getNext("P001");
        assertThat(result.nextExam()).isNull();
        assertThat(result.message()).isEqualTo("所有檢查已完成，謝謝您的配合！");
        verify(repository, never()).findIncompletePrerequisiteIds(anyString(), anyString());
    }
    @Test void noAssignedExamsMatchesZeroTotalCompletedContract() {
        exams();
        assertThat(service("10:30").getNext("P001").message()).isEqualTo("所有檢查已完成，謝謝您的配合！");
    }
    @Test void pendingButAllClosed() {
        exams(exam("E001", ExamStatus.PENDING, "08:00", "11:00"));
        var result = service("23:00").getNext("P001");
        assertThat(result.nextExam()).isNull();
        assertThat(result.message()).isEqualTo("目前沒有可進行的檢查，請洽工作人員");
    }
    @Test void rejectsInvalidDatabaseTimeRatherThanRecommendingIt() {
        exams(exam("E001", ExamStatus.PENDING, "25:00", "26:00"));
        assertThatThrownBy(() -> service("10:30").getNext("P001"))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }
}
