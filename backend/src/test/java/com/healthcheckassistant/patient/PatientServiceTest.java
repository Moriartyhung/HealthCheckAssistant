package com.healthcheckassistant.patient;
import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.patient.dto.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PatientServiceTest {
 private final PatientRepository repository = mock(PatientRepository.class);
 private final PatientService service = new PatientService(repository);
 @Test void countsCompletedAndPending() {
  when(repository.findPatientById("P001")).thenReturn(Optional.of(new PatientBasicData("P001", "測試受檢者A")));
  var exams = List.of(new ExamResponse("E001", "血液檢查", "S001", "抽血站", "08:00", "11:00", ExamStatus.PENDING, null), new ExamResponse("E002", "胸部X光", "S002", "X光站", "08:00", "16:00", ExamStatus.COMPLETED, LocalDateTime.of(2026,10,1,10,30)));
  when(repository.findExamsByPatientId("P001")).thenReturn(exams);
  var result = service.getExams("P001");
  assertThat(result.patientName()).isEqualTo("測試受檢者A");
  assertThat(result.completedCount()).isEqualTo(1);
  assertThat(result.totalCount()).isEqualTo(2);
  assertThat(result.exams()).isEqualTo(exams);
 }
 @Test void unknownPatientDoesNotQueryExams() {
  when(repository.findPatientById("P999")).thenReturn(Optional.empty());
  assertThatThrownBy(() -> service.getExams("P999")).isInstanceOf(NotFoundException.class).hasMessage("找不到受檢者");
  verify(repository, never()).findExamsByPatientId(anyString());
 }
 @Test void patientWithNoExamsHasZeroCounts() {
  when(repository.findPatientById("P002")).thenReturn(Optional.of(new PatientBasicData("P002", "測試受檢者B")));
  when(repository.findExamsByPatientId("P002")).thenReturn(List.of());
  var result = service.getExams("P002");
  assertThat(result.completedCount()).isZero(); assertThat(result.totalCount()).isZero(); assertThat(result.exams()).isEmpty();
 }
}
