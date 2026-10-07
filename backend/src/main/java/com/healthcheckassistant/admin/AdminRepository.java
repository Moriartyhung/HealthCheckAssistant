package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.AdminProgressResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbc;

    public AdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminProgressResponse> findAllProgress() {
        return jdbc.query("""
                SELECT P.PATIENT_ID, P.PATIENT_NAME,
                       COUNT(PE.EXAM_ID) AS TOTAL_COUNT,
                       NVL(SUM(CASE WHEN PE.STATUS = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS COMPLETED_COUNT
                FROM PATIENT P
                LEFT JOIN PATIENT_EXAM PE ON PE.PATIENT_ID = P.PATIENT_ID
                GROUP BY P.PATIENT_ID, P.PATIENT_NAME
                ORDER BY P.PATIENT_ID ASC
                """, (rs, rowNum) -> new AdminProgressResponse(
                rs.getString("PATIENT_ID"), rs.getString("PATIENT_NAME"),
                rs.getInt("COMPLETED_COUNT"), rs.getInt("TOTAL_COUNT")));
    }
}
