package com.healthcheckassistant.admin;

import com.healthcheckassistant.patient.PatientBasicData;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PatientManagementRepositoryTest {
    @Test void mapsListAndSinglePatientAndBindsIdRatherThanInterpolatingIt() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var rs = mock(ResultSet.class);
        when(rs.getString("PATIENT_ID")).thenReturn("P900");
        when(rs.getString("PATIENT_NAME")).thenReturn("姓名");
        when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(call ->
                List.of(((RowMapper<?>) call.getArgument(1)).mapRow(rs, 0)));
        String input = "' OR '1'='1";
        when(jdbc.query(anyString(), any(RowMapper.class), eq(input))).thenAnswer(call ->
                List.of(((RowMapper<?>) call.getArgument(1)).mapRow(rs, 0)));
        var repository = new PatientManagementRepository(jdbc);
        assertThat(repository.findAll()).containsExactly(new PatientBasicData("P900", "姓名"));
        assertThat(repository.findById(input)).contains(new PatientBasicData("P900", "姓名"));
        verify(jdbc).query(contains("ORDER BY PATIENT_ID"), any(RowMapper.class));
        verify(jdbc).query(contains("WHERE PATIENT_ID = ?"), any(RowMapper.class), eq(input));
    }
}
