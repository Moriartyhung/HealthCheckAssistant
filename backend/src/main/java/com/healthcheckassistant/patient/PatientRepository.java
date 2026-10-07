package com.healthcheckassistant.patient;

import com.healthcheckassistant.patient.dto.ExamResponse;
import com.healthcheckassistant.patient.dto.ExamStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.healthcheckassistant.common.exam.ExamPrerequisiteQueries;
import java.util.List;
import java.util.Optional;

@Repository
public class PatientRepository {
    private final JdbcTemplate jdbc;

    public PatientRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PatientBasicData> findPatientById(String patientId) {
        return jdbc.query("""
                SELECT PATIENT_ID, PATIENT_NAME
                FROM PATIENT
                WHERE PATIENT_ID = ?
                """, (rs, rowNum) -> new PatientBasicData(
                rs.getString("PATIENT_ID"), rs.getString("PATIENT_NAME")), patientId)
                .stream().findFirst();
    }

    public List<ExamResponse> findExamsByPatientId(String patientId) {
        return jdbc.query("""
                SELECT E.EXAM_ID, E.EXAM_NAME, E.STATION_ID, S.STATION_NAME,
                       E.START_TIME, E.END_TIME, PE.STATUS, PE.COMPLETED_AT
                FROM PATIENT_EXAM PE
                JOIN EXAM_ITEM E ON E.EXAM_ID = PE.EXAM_ID
                JOIN CHECK_STATION S ON S.STATION_ID = E.STATION_ID
                WHERE PE.PATIENT_ID = ?
                ORDER BY E.EXAM_ID
                """, (rs, rowNum) -> {
            var completedAt = rs.getTimestamp("COMPLETED_AT");
            return new ExamResponse(rs.getString("EXAM_ID"), rs.getString("EXAM_NAME"),
                    rs.getString("STATION_ID"), rs.getString("STATION_NAME"),
                    rs.getString("START_TIME"), rs.getString("END_TIME"),
                    ExamStatus.valueOf(rs.getString("STATUS")),
                    completedAt == null ? null : completedAt.toLocalDateTime());
        }, patientId);
    }
    public List<String> findIncompletePrerequisiteIds(String patientId, String examId) {
        return ExamPrerequisiteQueries.findIncompleteIds(jdbc, patientId, examId);
    }
}
