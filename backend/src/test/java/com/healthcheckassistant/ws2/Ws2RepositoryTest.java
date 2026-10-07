package com.healthcheckassistant.ws2;

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

class Ws2RepositoryTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final Ws2Repository repository=new Ws2Repository(jdbc);
    @Test void confirmationMapsNullAndTimestampAndBindsCode() {
        var created=LocalDateTime.of(2026,10,1,10,30); var used=created.plusMinutes(5);
        when(jdbc.query(anyString(),any(RowMapper.class),eq("codeA"))).thenAnswer(invocation -> {
            var rs=mock(ResultSet.class); when(rs.getString("CODE1")).thenReturn("codeA"); when(rs.getString("PATIENT_ID")).thenReturn("P001"); when(rs.getString("EXAM_ID")).thenReturn("E002");
            when(rs.getTimestamp("CREATED_AT")).thenReturn(Timestamp.valueOf(created)); when(rs.getTimestamp("USED_AT")).thenReturn(null,Timestamp.valueOf(used));
            RowMapper<?> mapper=invocation.getArgument(1); return List.of(mapper.mapRow(rs,0));
        });
        assertThat(repository.findConfirmation("codeA")).contains(new ConfirmationData("codeA","P001","E002",created,null));
        verify(jdbc).query(argThat(sql -> sql.contains("FROM CHECK_CONFIRMATION") && sql.contains("WHERE CODE1 = ?") && !sql.contains("codeA")),any(RowMapper.class),eq("codeA"));
    }
    @Test void usedConfirmationMapsTimestamp() {
        var used=LocalDateTime.of(2026,10,1,10,35);
        when(jdbc.query(anyString(),any(RowMapper.class),eq("codeA"))).thenAnswer(invocation -> {
            var rs=mock(ResultSet.class); when(rs.getString("CODE1")).thenReturn("codeA"); when(rs.getString("PATIENT_ID")).thenReturn("P001"); when(rs.getString("EXAM_ID")).thenReturn("E002");
            when(rs.getTimestamp("CREATED_AT")).thenReturn(Timestamp.valueOf(used.minusMinutes(5))); when(rs.getTimestamp("USED_AT")).thenReturn(Timestamp.valueOf(used));
            RowMapper<?> mapper=invocation.getArgument(1); return List.of(mapper.mapRow(rs,0));
        });
        assertThat(repository.findConfirmation("codeA").orElseThrow().usedAt()).isEqualTo(used);
    }
    @Test void missingConfirmationReturnsEmpty() {
        when(jdbc.query(anyString(),any(RowMapper.class),eq("missing"))).thenReturn(List.of());
        assertThat(repository.findConfirmation("missing")).isEmpty();
    }
    @Test void patientExamMapsStatusAndCompletedAtAndBindsCompositeKey() {
        var time=LocalDateTime.of(2026,10,1,10,35);
        when(jdbc.query(anyString(),any(RowMapper.class),eq("P001"),eq("E002"))).thenAnswer(invocation -> {
            var rs=mock(ResultSet.class); when(rs.getString("STATUS")).thenReturn("COMPLETED"); when(rs.getTimestamp("COMPLETED_AT")).thenReturn(Timestamp.valueOf(time));
            RowMapper<?> mapper=invocation.getArgument(1); return List.of(mapper.mapRow(rs,0));
        });
        assertThat(repository.findPatientExam("P001","E002")).contains(new PatientExamCompletionData(ExamStatus.COMPLETED,time));
        verify(jdbc).query(argThat(sql -> sql.contains("FROM PATIENT_EXAM") && sql.contains("PATIENT_ID = ?") && sql.contains("EXAM_ID = ?")),any(RowMapper.class),eq("P001"),eq("E002"));
    }
    @Test void missingPatientExamReturnsEmpty() {
        when(jdbc.query(anyString(),any(RowMapper.class),eq("P001"),eq("E002"))).thenReturn(List.of());
        assertThat(repository.findPatientExam("P001","E002")).isEmpty();
    }
    @Test void tokenClaimIncludesUnusedConditionAndReturnsAffectedRows() {
        when(jdbc.update(anyString(),eq("codeA"))).thenReturn(1,0);
        assertThat(repository.markConfirmationUsed("codeA")).isEqualTo(1); assertThat(repository.markConfirmationUsed("codeA")).isZero();
        verify(jdbc,times(2)).update(argThat(sql -> sql.contains("UPDATE CHECK_CONFIRMATION") && sql.contains("SET USED_AT = SYSTIMESTAMP") && sql.contains("WHERE CODE1 = ?") && sql.contains("AND USED_AT IS NULL")),eq("codeA"));
    }
    @Test void examCompletionIncludesPendingConditionAndUpdatesBothFields() {
        when(jdbc.update(anyString(),eq("P001"),eq("E002"))).thenReturn(1,0);
        assertThat(repository.completeExam("P001","E002")).isEqualTo(1); assertThat(repository.completeExam("P001","E002")).isZero();
        verify(jdbc,times(2)).update(argThat(sql -> sql.contains("UPDATE PATIENT_EXAM") && sql.contains("STATUS = 'COMPLETED'") && sql.contains("COMPLETED_AT = SYSTIMESTAMP") && sql.contains("PATIENT_ID = ?") && sql.contains("EXAM_ID = ?") && sql.contains("AND STATUS = 'PENDING'")),eq("P001"),eq("E002"));
    }
}
