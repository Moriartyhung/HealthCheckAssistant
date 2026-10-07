package com.healthcheckassistant.ws1;

import com.healthcheckassistant.patient.dto.ExamStatus;
import com.healthcheckassistant.ws1.dto.RemainingExamResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.healthcheckassistant.common.exam.ExamEligibility;
import com.healthcheckassistant.common.exam.ExamPrerequisiteQueries;
import java.util.List;

@Repository
public class Ws1Repository {
    private final JdbcTemplate jdbc;

    public Ws1Repository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean patientExists(String patientId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT WHERE PATIENT_ID = ?",
                Integer.class, patientId);
        return count != null && count > 0;
    }

    public boolean stationExists(String stationId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM CHECK_STATION WHERE STATION_ID = ?",
                Integer.class, stationId);
        return count != null && count > 0;
    }

    public List<PatientExamAtStation> findPatientExamAtStation(String patientId, String stationId) {
        return jdbc.query("""
                SELECT E.EXAM_ID, E.EXAM_NAME, PE.STATUS, E.START_TIME, E.END_TIME
                FROM PATIENT_EXAM PE
                JOIN EXAM_ITEM E ON E.EXAM_ID = PE.EXAM_ID
                WHERE PE.PATIENT_ID = ? AND E.STATION_ID = ?
                ORDER BY E.EXAM_ID
                """, (rs, rowNum) -> new PatientExamAtStation(
                rs.getString("EXAM_ID"), rs.getString("EXAM_NAME"),
                ExamStatus.valueOf(rs.getString("STATUS")),
                ExamEligibility.parseTime(rs.getString("START_TIME")),
                ExamEligibility.parseTime(rs.getString("END_TIME"))), patientId, stationId);
    }

    public List<RemainingExamResponse> findPendingExams(String patientId) {
        return jdbc.query("""
                SELECT E.EXAM_ID, E.EXAM_NAME
                FROM PATIENT_EXAM PE
                JOIN EXAM_ITEM E ON E.EXAM_ID = PE.EXAM_ID
                WHERE PE.PATIENT_ID = ? AND PE.STATUS = 'PENDING'
                ORDER BY E.EXAM_ID
                """, (rs, rowNum) -> new RemainingExamResponse(
                rs.getString("EXAM_ID"), rs.getString("EXAM_NAME")), patientId);
    }

    public List<String> findIncompletePrerequisiteIds(String patientId, String examId) {
        return ExamPrerequisiteQueries.findIncompleteIds(jdbc, patientId, examId);
    }

    public void insertConfirmation(String code1, String patientId, String examId) {
        int inserted = jdbc.update("""
                INSERT INTO CHECK_CONFIRMATION (CODE1, PATIENT_ID, EXAM_ID, CREATED_AT, USED_AT)
                VALUES (?, ?, ?, SYSTIMESTAMP, NULL)
                """, code1, patientId, examId);
        if (inserted != 1) {
            throw new IllegalStateException("Confirmation insert did not write exactly one row");
        }
    }
}

