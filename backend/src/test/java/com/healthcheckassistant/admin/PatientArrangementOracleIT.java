package com.healthcheckassistant.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthcheckassistant.admin.dto.ExamCatalogItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Opt-in real Oracle. Never changes existing patients or reference tables.
// CRUD uses a rollback test transaction. Failure injection uses a real HTTP request
// with its own server transaction so rollback is observable immediately via JDBC.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class PatientArrangementOracleIT {
    @Autowired PatientManagementService service;
    @MockitoSpyBean PatientManagementRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired TestRestTemplate http;

    List<String> usable(int count) {
        var catalog = service.catalog();
        Set<String> ids = new LinkedHashSet<>(); Set<String> stations = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (ExamCatalogItem exam : catalog) if (!ids.contains(exam.examId()) && !stations.contains(exam.stationId()) && ids.containsAll(exam.requiredExamIds())) {
                ids.add(exam.examId()); stations.add(exam.stationId()); changed = true;
                if (ids.size() == count) return List.copyOf(ids);
            }
        } while (changed);
        assumeTrue(false, "Real catalog lacks " + count + " compatible prerequisite-complete exams at distinct stations");
        return List.of();
    }
    String newId() { return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 18); }
    String idsJson(List<String> ids) { return "[\"" + String.join("\",\"", ids) + "\"]"; }

    @Test @Transactional void realArrangementCrudLeavesExistingPatientsUntouched() throws Exception {
        var exams = usable(3); String id = newId();
        mvc.perform(post("/api/admin/patients").contentType("application/json")
                .content("{\"patientId\":\"" + id + "\",\"patientName\":\"安排測試\",\"examIds\":" + idsJson(exams.subList(0, 2)) + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.exams.length()").value(2));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT_EXAM WHERE PATIENT_ID = ? AND STATUS = 'PENDING' AND COMPLETED_AT IS NULL", Integer.class, id)).isEqualTo(2);
        mvc.perform(get("/api/admin/patients/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.exams.length()").value(2));
        mvc.perform(put("/api/admin/patients/" + id).contentType("application/json")
                .content("{\"patientName\":\"姓名已改\",\"examIds\":" + idsJson(exams) + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exams.length()").value(3));
        mvc.perform(put("/api/admin/patients/" + id).contentType("application/json")
                .content("{\"patientName\":\"姓名已改\",\"examIds\":" + idsJson(exams.subList(0, 2)) + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exams.length()").value(2));
        mvc.perform(delete("/api/admin/patients/" + id)).andExpect(status().isConflict());
    }
    @Test void realServerTransactionRollsBackOnSecondAssignmentFailure() {
        var exams = usable(2).stream().sorted().toList(); String id = newId();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT WHERE PATIENT_ID = ?", Integer.class, id)).isZero();
        doThrow(new DataAccessResourceFailureException("intentional transaction failure"))
                .when(repository).insertAssignment(id, exams.get(1));
        try {
            var response = http.postForEntity("/api/admin/patients", Map.of("patientId", id, "patientName", "回滾測試", "examIds", exams), JsonNode.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT WHERE PATIENT_ID = ?", Integer.class, id)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PATIENT_EXAM WHERE PATIENT_ID = ?", Integer.class, id)).isZero();
        } finally {
            // Only this unique test ID is eligible for cleanup if a regression left partial data.
            jdbc.update("DELETE FROM PATIENT_EXAM WHERE PATIENT_ID = ?", id);
            jdbc.update("DELETE FROM PATIENT WHERE PATIENT_ID = ?", id);
        }
    }
}
