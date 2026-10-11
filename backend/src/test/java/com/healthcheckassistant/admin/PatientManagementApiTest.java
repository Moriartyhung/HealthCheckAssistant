package com.healthcheckassistant.admin;

import com.healthcheckassistant.patient.PatientBasicData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real HTTP mapping, validation, service and repository; only external Oracle JDBC is doubled.
@SpringBootTest(properties = "spring.datasource.password=")
@AutoConfigureMockMvc
class PatientManagementApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbc;
    @MockitoBean PlatformTransactionManager transactions;
    @BeforeEach void transactions() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    private void lengths() {
        when(jdbc.queryForObject(eq("SELECT LENGTHB(?) FROM DUAL"), eq(Integer.class), anyString())).thenReturn(12);
    }
    private void found() {
        when(jdbc.query(contains("FROM PATIENT WHERE"), any(RowMapper.class), eq("P900")))
                .thenReturn(List.of(new PatientBasicData("P900", "測試受檢者")));
    }
    @Test void createsPatientWithoutAssigningExams() throws Exception {
        lengths();
        when(jdbc.update(contains("INSERT INTO PATIENT "), eq("P900"), eq("測試受檢者"))).thenReturn(1);
        mvc.perform(post("/api/admin/patients").contentType("application/json")
                .content("{\"patientId\":\"P900\",\"patientName\":\"測試受檢者\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/admin/patients/P900"))
                .andExpect(jsonPath("$.patientId").value("P900")).andExpect(jsonPath("$.patientName").value("測試受檢者"));
        verify(jdbc).update(contains("INSERT INTO PATIENT "), eq("P900"), eq("測試受檢者"));
    }
    @Test void listsPatientsAndFindsOne() throws Exception {
        when(jdbc.query(contains("FROM PATIENT ORDER BY"), any(RowMapper.class))).thenReturn(List.of(new PatientBasicData("P900", "測試受檢者")));
        found();
        mvc.perform(get("/api/admin/patients")).andExpect(status().isOk()).andExpect(jsonPath("$[0].patientId").value("P900"));
        mvc.perform(get("/api/admin/patients/P900")).andExpect(status().isOk()).andExpect(jsonPath("$.patientName").value("測試受檢者"));
    }
    @Test void emptyListAndMissingPatient() throws Exception {
        mvc.perform(get("/api/admin/patients")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/admin/patients/P900")).andExpect(status().isNotFound());
    }
    @Test void duplicateInsertIncludingRaceReturnsConflict() throws Exception {
        lengths();
        when(jdbc.update(contains("INSERT INTO PATIENT "), anyString(), anyString())).thenThrow(new DuplicateKeyException("private SQL"));
        mvc.perform(post("/api/admin/patients").contentType("application/json").content("{\"patientId\":\"P900\",\"patientName\":\"測試\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("病歷號已存在"));
    }
    @Test void updatesOnlyName() throws Exception {
        lengths(); found();
        when(jdbc.update(contains("UPDATE PATIENT SET PATIENT_NAME"), eq("新姓名"), eq("P900"))).thenReturn(1);
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content("{\"patientName\":\"新姓名\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").value("P900")).andExpect(jsonPath("$.patientName").value("新姓名"));
        verify(jdbc).update(contains("UPDATE PATIENT SET PATIENT_NAME"), eq("新姓名"), eq("P900"));
    }
    @Test void rejectsChangingIdAndInvalidBodiesBeforeJdbc() throws Exception {
        for (String body : List.of("{\"patientId\":\"P901\",\"patientName\":\"姓名\"}", "{\"patientName\":\"   \"}", "{\"patientName\":12}", "{}")) {
            mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/admin/patients").contentType("application/json").content("{\"patientId\":\"\",\"patientName\":\"姓名\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(jdbc);
    }
    @Test void rejectsNamesExceedingOracleByteLimit() throws Exception {
        lengths();
        when(jdbc.queryForObject(eq("SELECT LENGTHB(?) FROM DUAL"), eq(Integer.class), eq("長".repeat(34)))).thenReturn(102);
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content("{\"patientName\":\"" + "長".repeat(34) + "\"}"))
                .andExpect(status().isBadRequest());
        verify(jdbc, never()).update(anyString(), anyString(), anyString());
    }
    @Test void deletesUnreferencedPatient() throws Exception {
        when(jdbc.update(contains("DELETE FROM PATIENT"), eq("P900"), eq("P900"), eq("P900"))).thenReturn(1);
        mvc.perform(delete("/api/admin/patients/P900")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("受檢者已刪除"));
        verify(jdbc).update(argThat((String sql) -> sql.contains("NOT EXISTS") && sql.contains("PATIENT_EXAM") && sql.contains("CHECK_CONFIRMATION")), eq("P900"), eq("P900"), eq("P900"));
    }
    @Test void refusesReferencedPatientAndForeignKeyRace() throws Exception {
        found();
        mvc.perform(delete("/api/admin/patients/P900")).andExpect(status().isConflict());
        when(jdbc.update(contains("DELETE FROM PATIENT"), eq("P900"), eq("P900"), eq("P900")))
                .thenThrow(new DataIntegrityViolationException("ORA-02292 private"));
        mvc.perform(delete("/api/admin/patients/P900")).andExpect(status().isConflict());
    }
    @Test void missingUpdateAndDeleteReturn404() throws Exception {
        lengths();
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content("{\"patientName\":\"姓名\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/patients/P900")).andExpect(status().isNotFound());
    }
    @Test void preexistingIdIsRejectedBeforeInsert() throws Exception {
        lengths(); found();
        mvc.perform(post("/api/admin/patients").contentType("application/json").content("{\"patientId\":\"P900\",\"patientName\":\"姓名\"}"))
                .andExpect(status().isConflict());
        verify(jdbc, never()).update(anyString(), anyString(), anyString());
    }
    @Test void rejectsOversizedAndControlCharacterInputs() throws Exception {
        mvc.perform(post("/api/admin/patients").contentType("application/json")
                .content("{\"patientId\":\"" + "P".repeat(21) + "\",\"patientName\":\"姓名\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json")
                .content("{\"patientName\":\"" + "A".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/patients/P900").contentType("application/json").content("{\"patientName\":\"姓名\\n\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(jdbc);
    }
    @Test void rejectsIdsWithPathSeparatorsOrExcessBytes() throws Exception {
        for (String id : List.of("P/900", "P;900", ".", "..")) {
            mvc.perform(post("/api/admin/patients").contentType("application/json").content("{\"patientId\":\"" + id + "\",\"patientName\":\"姓名\"}"))
                    .andExpect(status().isBadRequest());
        }
        when(jdbc.queryForObject(eq("SELECT LENGTHB(?) FROM DUAL"), eq(Integer.class), eq("病歷號太長測試"))).thenReturn(21);
        mvc.perform(post("/api/admin/patients").contentType("application/json").content("{\"patientId\":\"病歷號太長測試\",\"patientName\":\"姓名\"}"))
                .andExpect(status().isBadRequest());
        verify(jdbc, never()).update(anyString(), anyString(), anyString());
    }
    @Test void databaseOutageIsNeverReportedAsSuccess() throws Exception {
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private Oracle failure"));
        mvc.perform(get("/api/admin/patients")).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"message\":\"服務暫時無法使用\"}"));
    }
}
