package com.healthcheckassistant.patient;
import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.patient.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PatientController.class)
class PatientControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean PatientService service;
 @Test void returnsContractIncludingNullAndIsoTimestamp() throws Exception {
  when(service.getExams("P001")).thenReturn(new PatientProgressResponse("P001", "測試受檢者A", 1, 2, List.of(new ExamResponse("E001", "血液檢查", "S001", "抽血站", "08:00", "11:00", ExamStatus.PENDING, null),new ExamResponse("E002", "胸部X光", "S002", "X光站", "08:00", "16:00", ExamStatus.COMPLETED, LocalDateTime.of(2026,10,1,10,30)))));
  mvc.perform(get("/api/patients/P001/exams")).andExpect(status().isOk()).andExpect(content().json("""
  {"patientId":"P001","patientName":"測試受檢者A","completedCount":1,"totalCount":2,"exams":[
  {"examId":"E001","examName":"血液檢查","stationId":"S001","stationName":"抽血站","startTime":"08:00","endTime":"11:00","status":"PENDING","completedAt":null},
  {"examId":"E002","examName":"胸部X光","stationId":"S002","stationName":"X光站","startTime":"08:00","endTime":"16:00","status":"COMPLETED","completedAt":"2026-10-01T10:30:00"}]}
  """, true));
 }
 @Test void returns404Message() throws Exception {
  when(service.getExams("P999")).thenThrow(new NotFoundException("找不到受檢者"));
  mvc.perform(get("/api/patients/P999/exams")).andExpect(status().isNotFound()).andExpect(content().json("{\"message\":\"找不到受檢者\"}", true));
 }
 @Test void unexpectedErrorDoesNotExposeDatabaseDetails() throws Exception {
  when(service.getExams("P001")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("sensitive SQL and connection details"));
  mvc.perform(get("/api/patients/P001/exams")).andExpect(status().isInternalServerError()).andExpect(content().json("{\"message\":\"服務暫時無法使用\"}", true));
 }
 @Test void rejectsBlankAndOversizedPatientId() throws Exception {
  mvc.perform(get("/api/patients/{patientId}/exams", " ")).andExpect(status().isBadRequest()).andExpect(content().json("{\"message\":\"請求參數不正確\"}", true));
  mvc.perform(get("/api/patients/123456789012345678901/exams")).andExpect(status().isBadRequest());
  verifyNoInteractions(service);
 }
 @Test void doesNotRestrictPatientIdToSeedPattern() throws Exception {
  when(service.getExams("ABC-42")).thenReturn(new PatientProgressResponse("ABC-42", "其他受檢者", 0, 0, List.of()));
  mvc.perform(get("/api/patients/ABC-42/exams")).andExpect(status().isOk());
 }
}
