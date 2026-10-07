package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.AdminProgressResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminRepositoryTest {
    @Test void mapsCountsIncludingZeroExamPatientUsingOneOrderedAggregation() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            var rs = mock(ResultSet.class);
            when(rs.getString("PATIENT_ID")).thenReturn("P001", "P003");
            when(rs.getString("PATIENT_NAME")).thenReturn("測試受檢者A", "尚未安排");
            when(rs.getInt("COMPLETED_COUNT")).thenReturn(2, 0);
            when(rs.getInt("TOTAL_COUNT")).thenReturn(5, 0);
            RowMapper<?> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(rs, 0), mapper.mapRow(rs, 1));
        });
        assertThat(new AdminRepository(jdbc).findAllProgress()).containsExactly(
                new AdminProgressResponse("P001", "測試受檢者A", 2, 5),
                new AdminProgressResponse("P003", "尚未安排", 0, 0));
        verify(jdbc).query(argThat((String sql) -> sql.contains("FROM PATIENT P")
                && sql.contains("LEFT JOIN PATIENT_EXAM PE ON PE.PATIENT_ID = P.PATIENT_ID")
                && sql.contains("COUNT(PE.EXAM_ID) AS TOTAL_COUNT")
                && sql.contains("NVL(SUM(CASE WHEN PE.STATUS = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS COMPLETED_COUNT")
                && sql.contains("GROUP BY P.PATIENT_ID, P.PATIENT_NAME")
                && sql.contains("ORDER BY P.PATIENT_ID ASC")), any(RowMapper.class));
        verifyNoMoreInteractions(jdbc);
    }
    @Test void emptyPatientTableReturnsEmptyList() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        assertThat(new AdminRepository(jdbc).findAllProgress()).isEmpty();
    }
}
