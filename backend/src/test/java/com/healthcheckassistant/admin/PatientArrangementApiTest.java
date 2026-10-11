package com.healthcheckassistant.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real HTTP/controller/service/repository. The external JDBC resource and transaction
// manager are doubles with state snapshots; this is NOT an Oracle integration test.
@SpringBootTest(properties = "spring.datasource.password=")
@AutoConfigureMockMvc
class PatientArrangementApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbc;
    @MockitoBean PlatformTransactionManager transactions;
    boolean exists;
    String name;
    Map<String, String> arranged;
    Set<String> activeTokens;
    Set<String> historicalTokens;
    boolean failSecondInsert;
    boolean foreignKeyRace;
    boolean snapshotExists;
    String snapshotName;
    Map<String, String> snapshot;
    final LocalDateTime completed = LocalDateTime.of(2026, 10, 9, 10, 30);

    @BeforeEach void setup() {
        exists = false; name = "原姓名"; arranged = new LinkedHashMap<>();
        activeTokens = new HashSet<>(); historicalTokens = new HashSet<>();
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            snapshotExists = exists; snapshotName = name; snapshot = new LinkedHashMap<>(arranged);
            return new SimpleTransactionStatus();
        });
        doAnswer(call -> { exists = snapshotExists; name = snapshotName; arranged = new LinkedHashMap<>(snapshot); return null; })
                .when(transactions).rollback(any());
        when(jdbc.queryForObject(eq("SELECT LENGTHB(?) FROM DUAL"), eq(Integer.class), anyString())).thenReturn(12);
        when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(call -> rows(call.getArgument(0), call.getArgument(1)));
        when(jdbc.query(anyString(), any(RowMapper.class), eq("P900"))).thenAnswer(call -> rows(call.getArgument(0), call.getArgument(1)));
        when(jdbc.update(contains("INSERT INTO PATIENT "), eq("P900"), anyString())).thenAnswer(call -> {exists = true; name = call.getArgument(2); return 1;});
        when(jdbc.update(contains("UPDATE PATIENT SET PATIENT_NAME"), anyString(), eq("P900"))).thenAnswer(call -> {name = call.getArgument(1); return exists ? 1 : 0;});
        when(jdbc.update(contains("INSERT INTO PATIENT_EXAM"), eq("P900"), anyString())).thenAnswer(call -> {
            String exam = call.getArgument(2);
            if (failSecondInsert && exam.equals("E2")) throw new DataAccessResourceFailureException("simulated second insert failure");
            arranged.put(exam, "PENDING"); return 1;
        });
        when(jdbc.update(contains("DELETE FROM PATIENT_EXAM"), eq("P900"), anyString(), eq("P900"), anyString())).thenAnswer(call -> {
            if (foreignKeyRace) throw new DataIntegrityViolationException("simulated concurrent confirmation FK");
            String exam = call.getArgument(2);
            if (!"PENDING".equals(arranged.get(exam)) || activeTokens.contains(exam) || historicalTokens.contains(exam)) return 0;
            arranged.remove(exam); return 1;
        });
    }
    List<?> rows(String sql, RowMapper<?> mapper) throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        if (sql.contains("FROM PATIENT WHERE") && exists) data.add(Map.of("PATIENT_ID", "P900", "PATIENT_NAME", name));
        else if (sql.contains("FROM EXAM_ITEM E")) for (String id : List.of("E1", "E2", "E3", "E4")) data.add(examRow(id));
        else if (sql.contains("FROM EXAM_PREREQUISITE")) data.add(Map.of("EXAM_ID", "E2", "REQUIRED_EXAM_ID", "E1"));
        else if (sql.contains("FROM PATIENT_EXAM PE")) for (var entry : arranged.entrySet()) {
            var row = examRow(entry.getKey()); row.put("STATUS", entry.getValue());
            if (entry.getValue().equals("COMPLETED")) row.put("COMPLETED_AT", Timestamp.valueOf(completed));
            row.put("CONFIRMATION_COUNT", activeTokens.contains(entry.getKey()) || historicalTokens.contains(entry.getKey()) ? 1 : 0);
            row.put("ACTIVE_CONFIRMATION_COUNT", activeTokens.contains(entry.getKey()) ? 1 : 0);
            data.add(row);
        }
        List<Object> result = new ArrayList<>();
        for (var row : data) {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString(anyString())).thenAnswer(c -> Objects.toString(row.get(c.getArgument(0)), null));
            when(rs.getInt(anyString())).thenAnswer(c -> row.getOrDefault(c.getArgument(0), 0));
            when(rs.getTimestamp(anyString())).thenAnswer(c -> row.get(c.getArgument(0)));
            result.add(mapper.mapRow(rs, result.size()));
        }
        return result;
    }
    Map<String, Object> examRow(String id) {
        return new HashMap<>(Map.of("EXAM_ID", id, "EXAM_NAME", "檢查" + id, "STATION_ID", id.equals("E4") ? "S1" : "S" + id.substring(1),
                "STATION_NAME", id.equals("E4") ? "站點1" : "站點" + id.substring(1), "START_TIME", "08:00", "END_TIME", "17:00"));
    }
    String create(String ids) { return "{\"patientId\":\"P900\",\"patientName\":\"新姓名\",\"examIds\":" + ids + "}"; }
    String update(String ids) { return "{\"patientName\":\"更新姓名\",\"examIds\":" + ids + "}"; }
    void existing(String... exams) { exists = true; for (String exam : exams) arranged.put(exam, "PENDING"); }

    @Test void catalogIncludesStationsAndPrerequisites() throws Exception {
        mvc.perform(get("/api/admin/exams")).andExpect(status().isOk())
                .andExpect(jsonPath("$[1].stationName").value("站點2")).andExpect(jsonPath("$[1].requiredExamIds[0]").value("E1"));
    }
    @Test void createsMultipleExamsAndReadsDetail() throws Exception {
        mvc.perform(post("/api/admin/patients").contentType("application/json").content(create("[\"E1\",\"E2\"]")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.exams.length()").value(2));
        mvc.perform(get("/api/admin/patients/P900")).andExpect(status().isOk())
                .andExpect(jsonPath("$.exams[0].stationName").value("站點1"))
                .andExpect(jsonPath("$.exams[0].status").value("PENDING")).andExpect(jsonPath("$.exams[0].completedAt").isEmpty());
        assertThat(arranged).containsEntry("E1", "PENDING").containsEntry("E2", "PENDING");
        verify(transactions).commit(any());
    }
    @Test void editsNameAndAddsExamWithoutTouchingCompletedStatus() throws Exception {
        existing("E1"); arranged.put("E1", "COMPLETED");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[\"E1\",\"E2\"]")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exams[0].completedAt").value(completed.toString() + ":00"));
        assertThat(name).isEqualTo("更新姓名"); assertThat(arranged).containsEntry("E1", "COMPLETED").containsEntry("E2", "PENDING");
    }
    @Test void removesPendingUnreferencedExam() throws Exception {
        existing("E1", "E3");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[\"E1\"]"))).andExpect(status().isOk());
        assertThat(arranged).containsOnlyKeys("E1");
    }
    @Test void rejectsRemovalOfCompletedExam() throws Exception {
        existing("E1"); arranged.put("E1", "COMPLETED");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[]"))).andExpect(status().isConflict());
        assertThat(name).isEqualTo("原姓名"); assertThat(arranged.get("E1")).isEqualTo("COMPLETED");
    }
    @Test void rejectsRemovalWithActiveOrHistoricalConfirmation() throws Exception {
        existing("E1"); activeTokens.add("E1");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[]"))).andExpect(status().isConflict());
        activeTokens.clear(); historicalTokens.add("E1");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[]"))).andExpect(status().isConflict());
        assertThat(arranged).containsKey("E1");
    }
    @Test void rejectsMissingPrerequisitesAndSameStationAssignments() throws Exception {
        for (String ids : List.of("[\"E2\"]", "[\"E1\",\"E4\"]")) {
            mvc.perform(post("/api/admin/patients").contentType("application/json").content(create(ids))).andExpect(status().isConflict());
            assertThat(exists).isFalse();
        }
        existing("E1", "E2");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[\"E2\"]"))).andExpect(status().isConflict());
        assertThat(arranged).containsOnlyKeys("E1", "E2");
    }
    @Test void rejectsDuplicateUnknownAndNonStringExamIds() throws Exception {
        for (String ids : List.of("[\"E1\",\"E1\"]", "[\"BAD\"]", "[12]", "[null]", "[\"\"]")) {
            mvc.perform(post("/api/admin/patients").contentType("application/json").content(create(ids))).andExpect(status().isBadRequest());
            assertThat(exists).isFalse();
        }
    }
    @Test void rejectsClientStatusAndCompletionTime() throws Exception {
        existing("E1");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json")
                .content("{\"patientName\":\"新姓名\",\"examIds\":[\"E1\"],\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest());
        assertThat(arranged.get("E1")).isEqualTo("PENDING");
    }
    @Test void failedSecondInsertRollsBackPatientAndFirstAssignment() throws Exception {
        failSecondInsert = true;
        mvc.perform(post("/api/admin/patients").contentType("application/json").content(create("[\"E1\",\"E2\"]"))).andExpect(status().isInternalServerError());
        assertThat(exists).isFalse(); assertThat(arranged).isEmpty(); verify(transactions).rollback(any()); verify(transactions, never()).commit(any());
    }
    @Test void failedEditRestoresNameAndRemovedAssignments() throws Exception {
        existing("E1", "E3"); failSecondInsert = true;
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[\"E1\",\"E2\"]"))).andExpect(status().isInternalServerError());
        assertThat(name).isEqualTo("原姓名"); assertThat(arranged).containsOnlyKeys("E1", "E3"); verify(transactions).rollback(any());
    }
    @Test void concurrentConfirmationForeignKeyRestoresEntireEdit() throws Exception {
        existing("E1", "E3"); foreignKeyRace = true;
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(update("[\"E1\"]"))).andExpect(status().isConflict());
        assertThat(name).isEqualTo("原姓名"); assertThat(arranged).containsOnlyKeys("E1", "E3"); verify(transactions).rollback(any());
    }
    @Test void rejectsCyclicPrerequisiteConfiguration() throws Exception {
        when(jdbc.query(contains("FROM EXAM_PREREQUISITE"), any(RowMapper.class))).thenAnswer(call -> {
            var mapper = (RowMapper<?>) call.getArgument(1);
            var a = mock(ResultSet.class); when(a.getString("EXAM_ID")).thenReturn("E1"); when(a.getString("REQUIRED_EXAM_ID")).thenReturn("E2");
            var b = mock(ResultSet.class); when(b.getString("EXAM_ID")).thenReturn("E2"); when(b.getString("REQUIRED_EXAM_ID")).thenReturn("E1");
            return List.of(mapper.mapRow(a, 0), mapper.mapRow(b, 1));
        });
        mvc.perform(post("/api/admin/patients").contentType("application/json").content(create("[\"E1\",\"E2\"]")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("健檢前置條件存在循環，無法安排"));
        assertThat(exists).isFalse();
    }
    @Test void nameOnlyUpdatePreservesAssignmentsAndLocksPatient() throws Exception {
        existing("E1"); arranged.put("E1", "COMPLETED");
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content("{\"patientName\":\"僅改姓名\"}"))
                .andExpect(status().isOk());
        assertThat(arranged.get("E1")).isEqualTo("COMPLETED"); assertThat(name).isEqualTo("僅改姓名");
        verify(jdbc).query(contains("FOR UPDATE"), any(RowMapper.class), eq("P900"));
        verify(jdbc, never()).update(contains("INSERT INTO PATIENT_EXAM"), anyString(), anyString());
    }
    @Test void detailExplainsUnusedConfirmationProtection() throws Exception {
        existing("E1"); activeTokens.add("E1");
        mvc.perform(get("/api/admin/patients/P900")).andExpect(status().isOk())
                .andExpect(jsonPath("$.exams[0].removable").value(false))
                .andExpect(jsonPath("$.exams[0].removalBlockReason").value("已有有效 WS1 確認碼，不可移除"));
    }
}
