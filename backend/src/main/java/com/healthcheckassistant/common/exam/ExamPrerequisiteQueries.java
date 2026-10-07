package com.healthcheckassistant.common.exam;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

/** One parameterized query shared by the two read paths. */
public final class ExamPrerequisiteQueries {
    private ExamPrerequisiteQueries() {}

    public static List<String> findIncompleteIds(JdbcTemplate jdbc, String patientId, String examId) {
        // NOT EXISTS also blocks a prerequisite not assigned to this patient.
        return jdbc.queryForList("""
                SELECT R.REQUIRED_EXAM_ID
                FROM EXAM_PREREQUISITE R
                WHERE R.EXAM_ID = ? AND NOT EXISTS (
                    SELECT 1 FROM PATIENT_EXAM PE
                    WHERE PE.PATIENT_ID = ? AND PE.EXAM_ID = R.REQUIRED_EXAM_ID
                      AND PE.STATUS = 'COMPLETED'
                )
                ORDER BY R.REQUIRED_EXAM_ID
                """, String.class, examId, patientId);
    }
}
