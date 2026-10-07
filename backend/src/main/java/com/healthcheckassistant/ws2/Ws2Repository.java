package com.healthcheckassistant.ws2;

import com.healthcheckassistant.patient.dto.ExamStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class Ws2Repository {
    private final JdbcTemplate jdbc;

    public Ws2Repository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ConfirmationData> findConfirmation(String code1) {
        return jdbc.query("""
                SELECT CODE1, PATIENT_ID, EXAM_ID, CREATED_AT, USED_AT
                FROM CHECK_CONFIRMATION
                WHERE CODE1 = ?
                """, (rs, rowNum) -> {
            var usedAt = rs.getTimestamp("USED_AT");
            return new ConfirmationData(rs.getString("CODE1"), rs.getString("PATIENT_ID"),
                    rs.getString("EXAM_ID"), rs.getTimestamp("CREATED_AT").toLocalDateTime(),
                    usedAt == null ? null : usedAt.toLocalDateTime());
        }, code1).stream().findFirst();
    }

    public Optional<PatientExamCompletionData> findPatientExam(String patientId, String examId) {
        return jdbc.query("""
                SELECT STATUS, COMPLETED_AT
                FROM PATIENT_EXAM
                WHERE PATIENT_ID = ? AND EXAM_ID = ?
                """, (rs, rowNum) -> {
            var completedAt = rs.getTimestamp("COMPLETED_AT");
            return new PatientExamCompletionData(ExamStatus.valueOf(rs.getString("STATUS")),
                    completedAt == null ? null : completedAt.toLocalDateTime());
        }, patientId, examId).stream().findFirst();
    }

    public int markConfirmationUsed(String code1) {
        return jdbc.update("""
                UPDATE CHECK_CONFIRMATION
                SET USED_AT = SYSTIMESTAMP
                WHERE CODE1 = ? AND USED_AT IS NULL
                """, code1);
    }

    public int completeExam(String patientId, String examId) {
        return jdbc.update("""
                UPDATE PATIENT_EXAM
                SET STATUS = 'COMPLETED', COMPLETED_AT = SYSTIMESTAMP
                WHERE PATIENT_ID = ? AND EXAM_ID = ? AND STATUS = 'PENDING'
                """, patientId, examId);
    }
}
