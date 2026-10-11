package com.healthcheckassistant.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Opt-in real Oracle tests. MockMvc executes in this transaction; PATIENT changes roll back.
// No other tables are inserted, updated or deleted; existing related patients are read only.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class PatientManagementOracleIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test void realOracleCrudAndDuplicateProtection() throws Exception {
        String id = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        String body = "{\"patientId\":\"" + id + "\",\"patientName\":\"整合測試\"}";
        mvc.perform(post("/api/admin/patients").contentType("application/json").content(body)).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT PATIENT_NAME FROM PATIENT WHERE PATIENT_ID = ?", String.class, id)).isEqualTo("整合測試");
        mvc.perform(post("/api/admin/patients").contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(get("/api/admin/patients")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.patientId == '" + id + "')]").isNotEmpty());
        mvc.perform(get("/api/admin/patients/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.patientName").value("整合測試"));
        mvc.perform(put("/api/admin/patients/" + id).contentType("application/json").content("{\"patientName\":\"更新姓名\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT PATIENT_NAME FROM PATIENT WHERE PATIENT_ID = ?", String.class, id)).isEqualTo("更新姓名");
        mvc.perform(put("/api/admin/patients/" + id).contentType("application/json").content("{\"patientName\":\"" + "長".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/patients/" + id)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT WHERE PATIENT_ID = ?", Integer.class, id)).isZero();
        mvc.perform(get("/api/admin/patients/" + id)).andExpect(status().isNotFound());
    }
    @Test void refusesDeletionOfExistingRelatedPatient() throws Exception {
        var ids = jdbc.queryForList("SELECT PATIENT_ID FROM PATIENT_EXAM ORDER BY PATIENT_ID FETCH FIRST 1 ROWS ONLY", String.class);
        assumeFalse(ids.isEmpty(), "No existing related patient: association rejection requires fixture data");
        String id = ids.get(0);
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT_EXAM WHERE PATIENT_ID = ?", Integer.class, id);
        mvc.perform(delete("/api/admin/patients/" + id)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT WHERE PATIENT_ID = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT_EXAM WHERE PATIENT_ID = ?", Integer.class, id)).isEqualTo(before);
    }
    @Test void validatesActualOracleByteLengthWithinCharacterLimit() throws Exception {
        String name = "長".repeat(100);
        Integer bytes = jdbc.queryForObject("SELECT LENGTHB(?) FROM DUAL", Integer.class, name);
        org.junit.jupiter.api.Assumptions.assumeTrue(bytes != null && bytes > 100,
                "Database character set does not produce more than 100 bytes for this multibyte fixture");
        String id = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        mvc.perform(post("/api/admin/patients").contentType("application/json")
                .content("{\"patientId\":\"" + id + "\",\"patientName\":\"原姓名\"}")).andExpect(status().isCreated());
        mvc.perform(put("/api/admin/patients/" + id).contentType("application/json")
                .content("{\"patientName\":\"" + name + "\"}")).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT PATIENT_NAME FROM PATIENT WHERE PATIENT_ID = ?", String.class, id)).isEqualTo("原姓名");
    }
}
