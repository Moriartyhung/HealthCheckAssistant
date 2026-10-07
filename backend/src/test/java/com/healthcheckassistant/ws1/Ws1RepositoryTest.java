package com.healthcheckassistant.ws1;

import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.ws1.dto.RemainingExamResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Ws1RepositoryTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final Ws1Repository repository = new Ws1Repository(jdbc);
    @Test void patientAndStationExistenceUseBoundParameters() {
        when(jdbc.queryForObject(anyString(),eq(Integer.class),eq("P001"))).thenReturn(1);
        when(jdbc.queryForObject(anyString(),eq(Integer.class),eq("S002"))).thenReturn(1);
        assertThat(repository.patientExists("P001")).isTrue(); assertThat(repository.stationExists("S002")).isTrue();
        verify(jdbc).queryForObject(argThat(sql -> sql.contains("FROM PATIENT") && sql.contains("PATIENT_ID = ?") && !sql.contains("P001")),eq(Integer.class),eq("P001"));
        verify(jdbc).queryForObject(argThat(sql -> sql.contains("FROM CHECK_STATION") && sql.contains("STATION_ID = ?") && !sql.contains("S002")),eq(Integer.class),eq("S002"));
    }
    @Test void absentPatientAndStationReturnFalse() {
        when(jdbc.queryForObject(anyString(),eq(Integer.class),anyString())).thenReturn(0);
        assertThat(repository.patientExists("P999")).isFalse(); assertThat(repository.stationExists("S999")).isFalse();
    }
    @Test void stationQueryMapsAllRowsAndParsesTimes() {
        when(jdbc.query(anyString(),any(RowMapper.class),eq("P001"),eq("S002"))).thenAnswer(invocation -> {
            var rs=mock(ResultSet.class);
            when(rs.getString("EXAM_ID")).thenReturn("E002","E006"); when(rs.getString("EXAM_NAME")).thenReturn("胸部X光");
            when(rs.getString("STATUS")).thenReturn("PENDING","COMPLETED"); when(rs.getString("START_TIME")).thenReturn("08:00"); when(rs.getString("END_TIME")).thenReturn("16:00");
            RowMapper<?> mapper=invocation.getArgument(1); return List.of(mapper.mapRow(rs,0),mapper.mapRow(rs,1));
        });
        var rows=repository.findPatientExamAtStation("P001","S002");
        assertThat(rows).hasSize(2); assertThat(rows.get(0)).isEqualTo(new PatientExamAtStation("E002","胸部X光",ExamStatus.PENDING,LocalTime.of(8,0),LocalTime.of(16,0)));
        assertThat(rows.get(1).status()).isEqualTo(ExamStatus.COMPLETED);
        verify(jdbc).query(argThat(sql -> sql.contains("JOIN EXAM_ITEM") && sql.contains("PE.PATIENT_ID = ?") && sql.contains("E.STATION_ID = ?") && sql.contains("ORDER BY E.EXAM_ID")),any(RowMapper.class),eq("P001"),eq("S002"));
    }
    @Test void pendingQueryMapsMinimalDtoAndSortsByExamId() {
        when(jdbc.query(anyString(),any(RowMapper.class),eq("P001"))).thenAnswer(invocation -> {
            var rs=mock(ResultSet.class); when(rs.getString("EXAM_ID")).thenReturn("E001"); when(rs.getString("EXAM_NAME")).thenReturn("血液檢查");
            RowMapper<?> mapper=invocation.getArgument(1); return List.of(mapper.mapRow(rs,0));
        });
        assertThat(repository.findPendingExams("P001")).containsExactly(new RemainingExamResponse("E001","血液檢查"));
        verify(jdbc).query(argThat(sql -> sql.contains("PE.STATUS = 'PENDING'") && sql.contains("PE.PATIENT_ID = ?") && sql.contains("ORDER BY E.EXAM_ID")),any(RowMapper.class),eq("P001"));
    }
    @Test void prerequisitesRequireCompletedPatientRowIncludingMissingAssignments() {
        when(jdbc.queryForList(anyString(),eq(String.class),eq("E005"),eq("P001"))).thenReturn(List.of("E001","E003"));
        assertThat(repository.findIncompletePrerequisiteIds("P001","E005")).containsExactly("E001","E003");
        verify(jdbc).queryForList(argThat(sql -> sql.contains("FROM EXAM_PREREQUISITE") && sql.contains("NOT EXISTS") && sql.contains("PE.EXAM_ID = R.REQUIRED_EXAM_ID") && sql.contains("PE.STATUS = 'COMPLETED'") && sql.contains("PE.PATIENT_ID = ?")),eq(String.class),eq("E005"),eq("P001"));
    }
    @Test void confirmationInsertBindsValuesAndDoesNotChangePatientExam() {
        when(jdbc.update(anyString(),eq("uuid-token"),eq("P001"),eq("E002"))).thenReturn(1);
        repository.insertConfirmation("uuid-token","P001","E002");
        verify(jdbc).update(argThat(sql -> sql.contains("INSERT INTO CHECK_CONFIRMATION") && sql.contains("CREATED_AT, USED_AT") && sql.contains("SYSTIMESTAMP, NULL") && !sql.contains("UPDATE") && !sql.contains("uuid-token")),eq("uuid-token"),eq("P001"),eq("E002"));
    }
    @Test void unsuccessfulInsertThrowsInsteadOfPretendingPersistence() {
        when(jdbc.update(anyString(),anyString(),anyString(),anyString())).thenReturn(0);
        assertThatThrownBy(() -> repository.insertConfirmation("token","P001","E002")).isInstanceOf(IllegalStateException.class);
    }
}
