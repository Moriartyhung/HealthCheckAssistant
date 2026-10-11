package com.healthcheckassistant.admin;

import com.healthcheckassistant.patient.PatientBasicData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.healthcheckassistant.admin.dto.ExamCatalogItem;
import com.healthcheckassistant.admin.dto.ManagedPatientExam;
import com.healthcheckassistant.patient.dto.ExamStatus;

@Repository
public class PatientManagementRepository {
    private final JdbcTemplate jdbc;
    public PatientManagementRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<PatientBasicData> findAll() {
        return jdbc.query("SELECT PATIENT_ID, PATIENT_NAME FROM PATIENT ORDER BY PATIENT_ID",
                (rs, row) -> new PatientBasicData(rs.getString("PATIENT_ID"), rs.getString("PATIENT_NAME")));
    }
    public Optional<PatientBasicData> findById(String id) {
        return jdbc.query("SELECT PATIENT_ID, PATIENT_NAME FROM PATIENT WHERE PATIENT_ID = ?",
                (rs, row) -> new PatientBasicData(rs.getString("PATIENT_ID"), rs.getString("PATIENT_NAME")), id).stream().findFirst();
    }
    public int byteLength(String value) {
        // Use Oracle's actual database character set rather than assuming UTF-8.
        Integer length = jdbc.queryForObject("SELECT LENGTHB(?) FROM DUAL", Integer.class, value);
        if (length == null) throw new IllegalStateException("Oracle returned no string length");
        return length;
    }
    public int insert(String id, String name) {
        return jdbc.update("INSERT INTO PATIENT (PATIENT_ID, PATIENT_NAME) VALUES (?, ?)", id, name);
    }
    public int updateName(String id, String name) {
        return jdbc.update("UPDATE PATIENT SET PATIENT_NAME = ? WHERE PATIENT_ID = ?", name, id);
    }
    public int deleteUnreferenced(String id) {
        // Single atomic statement; Oracle FK constraints also protect against concurrent inserts.
        return jdbc.update("""
                DELETE FROM PATIENT
                WHERE PATIENT_ID = ?
                  AND NOT EXISTS (SELECT 1 FROM PATIENT_EXAM WHERE PATIENT_ID = ?)
                  AND NOT EXISTS (SELECT 1 FROM CHECK_CONFIRMATION WHERE PATIENT_ID = ?)
                """, id, id, id);
    }

    public Optional<PatientBasicData> lockPatient(String id) {
        return jdbc.query("SELECT PATIENT_ID, PATIENT_NAME FROM PATIENT WHERE PATIENT_ID = ? FOR UPDATE",
                (rs, row) -> new PatientBasicData(rs.getString("PATIENT_ID"), rs.getString("PATIENT_NAME")), id).stream().findFirst();
    }
    public void lockAssignments(String id) {
        // Serialize with WS2's status UPDATE before evaluating removal eligibility.
        jdbc.queryForList("SELECT EXAM_ID FROM PATIENT_EXAM WHERE PATIENT_ID = ? ORDER BY EXAM_ID FOR UPDATE", String.class, id);
    }
    public List<ManagedPatientExam> findAssignedExams(String id) {
        return jdbc.query("""
                SELECT E.EXAM_ID, E.EXAM_NAME, E.STATION_ID, S.STATION_NAME,
                       E.START_TIME, E.END_TIME, PE.STATUS, PE.COMPLETED_AT,
                       (SELECT COUNT(*) FROM CHECK_CONFIRMATION C
                        WHERE C.PATIENT_ID = PE.PATIENT_ID AND C.EXAM_ID = PE.EXAM_ID) AS CONFIRMATION_COUNT,
                       (SELECT COUNT(*) FROM CHECK_CONFIRMATION C
                        WHERE C.PATIENT_ID = PE.PATIENT_ID AND C.EXAM_ID = PE.EXAM_ID AND C.USED_AT IS NULL) AS ACTIVE_CONFIRMATION_COUNT
                FROM PATIENT_EXAM PE
                JOIN EXAM_ITEM E ON E.EXAM_ID = PE.EXAM_ID
                JOIN CHECK_STATION S ON S.STATION_ID = E.STATION_ID
                WHERE PE.PATIENT_ID = ? ORDER BY E.EXAM_ID
                """, (rs, row) -> {
            var status = ExamStatus.valueOf(rs.getString("STATUS"));
            var completed = rs.getTimestamp("COMPLETED_AT");
            String reason = status == ExamStatus.COMPLETED ? "已完成項目不可移除"
                    : rs.getInt("ACTIVE_CONFIRMATION_COUNT") > 0 ? "已有有效 WS1 確認碼，不可移除"
                    : rs.getInt("CONFIRMATION_COUNT") > 0 ? "已有確認碼紀錄，不可移除" : null;
            return new ManagedPatientExam(rs.getString("EXAM_ID"), rs.getString("EXAM_NAME"), rs.getString("STATION_ID"),
                    rs.getString("STATION_NAME"), rs.getString("START_TIME"), rs.getString("END_TIME"), status,
                    completed == null ? null : completed.toLocalDateTime(), reason == null, reason);
        }, id);
    }
    public List<ExamCatalogItem> findExamOptions() {
        return jdbc.query("""
                SELECT E.EXAM_ID, E.EXAM_NAME, E.STATION_ID, S.STATION_NAME, E.START_TIME, E.END_TIME
                FROM EXAM_ITEM E JOIN CHECK_STATION S ON S.STATION_ID = E.STATION_ID ORDER BY E.EXAM_ID
                """, (rs, row) -> new ExamCatalogItem(rs.getString("EXAM_ID"), rs.getString("EXAM_NAME"),
                rs.getString("STATION_ID"), rs.getString("STATION_NAME"), rs.getString("START_TIME"), rs.getString("END_TIME"), List.of()));
    }
    public record Prerequisite(String examId, String requiredExamId) {}
    public List<Prerequisite> findPrerequisites() {
        return jdbc.query("SELECT EXAM_ID, REQUIRED_EXAM_ID FROM EXAM_PREREQUISITE ORDER BY EXAM_ID, REQUIRED_EXAM_ID",
                (rs, row) -> new Prerequisite(rs.getString("EXAM_ID"), rs.getString("REQUIRED_EXAM_ID")));
    }
    public int insertAssignment(String id, String examId) {
        return jdbc.update("INSERT INTO PATIENT_EXAM (PATIENT_ID, EXAM_ID, STATUS, COMPLETED_AT) VALUES (?, ?, 'PENDING', NULL)", id, examId);
    }
    public int removePendingAssignment(String id, String examId) {
        return jdbc.update("""
                DELETE FROM PATIENT_EXAM
                WHERE PATIENT_ID = ? AND EXAM_ID = ? AND STATUS = 'PENDING'
                  AND NOT EXISTS (SELECT 1 FROM CHECK_CONFIRMATION WHERE PATIENT_ID = ? AND EXAM_ID = ?)
                """, id, examId, id, examId);
    }
}
