package com.healthcheckassistant.patient;
import com.healthcheckassistant.patient.dto.ExamStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class PatientRepositoryTest {
 @Test void bindsPatientIdAndMapsPatient() {
  var jdbc = mock(JdbcTemplate.class);
  var repository = new PatientRepository(jdbc);
  when(jdbc.query(anyString(), any(RowMapper.class), eq("P001"))).thenAnswer(invocation -> {
   var rs = mock(ResultSet.class);
   when(rs.getString("PATIENT_ID")).thenReturn("P001"); when(rs.getString("PATIENT_NAME")).thenReturn("測試受檢者A");
   RowMapper<?> mapper = invocation.getArgument(1); return List.of(mapper.mapRow(rs, 0));
  });
  assertThat(repository.findPatientById("P001")).contains(new PatientBasicData("P001", "測試受檢者A"));
  verify(jdbc).query(argThat(sql -> sql.contains("WHERE PATIENT_ID = ?") && !sql.contains("P001")), any(RowMapper.class), eq("P001"));
 }
 @Test void missingPatientReturnsEmpty() {
  var jdbc = mock(JdbcTemplate.class);
  when(jdbc.query(anyString(), any(RowMapper.class), eq("P999"))).thenReturn(List.of());
  assertThat(new PatientRepository(jdbc).findPatientById("P999")).isEmpty();
 }
 @Test void mapsNullAndCompletedTimestampWithOrderedBoundQuery() {
  var jdbc = mock(JdbcTemplate.class);
  var timestamp = LocalDateTime.of(2026,10,1,10,30,12,123456000);
  when(jdbc.query(anyString(), any(RowMapper.class), eq("P001"))).thenAnswer(invocation -> {
   var rs = mock(ResultSet.class);
   when(rs.getString("EXAM_ID")).thenReturn("E001", "E002"); when(rs.getString("EXAM_NAME")).thenReturn("血液檢查");
   when(rs.getString("STATION_ID")).thenReturn("S001"); when(rs.getString("STATION_NAME")).thenReturn("抽血站");
   when(rs.getString("START_TIME")).thenReturn("08:00"); when(rs.getString("END_TIME")).thenReturn("11:00");
   when(rs.getString("STATUS")).thenReturn("PENDING", "COMPLETED"); when(rs.getTimestamp("COMPLETED_AT")).thenReturn(null, Timestamp.valueOf(timestamp));
   RowMapper<?> mapper = invocation.getArgument(1); return List.of(mapper.mapRow(rs,0), mapper.mapRow(rs,1));
  });
  var exams = new PatientRepository(jdbc).findExamsByPatientId("P001");
  assertThat(exams.get(0).completedAt()).isNull(); assertThat(exams.get(0).status()).isEqualTo(ExamStatus.PENDING);
  assertThat(exams.get(1).completedAt()).isEqualTo(timestamp); assertThat(exams.get(1).status()).isEqualTo(ExamStatus.COMPLETED);
  assertThat(exams.get(0).examId()).isEqualTo("E001"); assertThat(exams.get(0).examName()).isEqualTo("血液檢查");
  assertThat(exams.get(0).stationId()).isEqualTo("S001"); assertThat(exams.get(0).stationName()).isEqualTo("抽血站");
  assertThat(exams.get(0).startTime()).isEqualTo("08:00"); assertThat(exams.get(0).endTime()).isEqualTo("11:00");
  verify(jdbc).query(argThat(sql -> sql.contains("WHERE PE.PATIENT_ID = ?") && sql.contains("ORDER BY E.EXAM_ID") && !sql.contains("P001")),any(RowMapper.class),eq("P001"));
 }
 @Test void prerequisiteQueryRequiresCompletedRowAndBindsExamThenPatient() {
  var jdbc = mock(JdbcTemplate.class);
  when(jdbc.queryForList(anyString(), eq(String.class), eq("E005"), eq("P001")))
          .thenReturn(List.of("E004"));
  assertThat(new PatientRepository(jdbc).findIncompletePrerequisiteIds("P001", "E005"))
          .containsExactly("E004");
  verify(jdbc).queryForList(argThat(sql -> sql.contains("NOT EXISTS")
          && sql.contains("PE.EXAM_ID = R.REQUIRED_EXAM_ID")
          && sql.contains("PE.STATUS = 'COMPLETED'")
          && sql.contains("WHERE R.EXAM_ID = ?")
          && sql.contains("PE.PATIENT_ID = ?")
          && !sql.contains("P001") && !sql.contains("E005")),
          eq(String.class), eq("E005"), eq("P001"));
  verifyNoMoreInteractions(jdbc);
 }
}
