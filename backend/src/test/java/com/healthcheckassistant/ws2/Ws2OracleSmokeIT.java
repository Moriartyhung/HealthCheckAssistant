package com.healthcheckassistant.ws2;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthcheckassistant.patient.dto.PatientProgressResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/**
 * MANUAL Oracle smoke only: mvnw.cmd -Dtest=Ws2OracleSmokeIT test in a session with DB_PASSWORD.
 * Maven's default test patterns exclude *IT. Uses real existing data; never inserts fake tokens.
 * If an unused token references a PENDING exam, this test completes it permanently (no reset).
 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Ws2OracleSmokeIT {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired Ws2Repository repository;

    @Test @Order(1) void invalidCodeQueriesRealOracleAndReturns404() {
        var result=http.postForEntity("/api/ws2/complete",Map.of("code1","INVALID-CODE-TEST"),JsonNode.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().get("message").asText()).isEqualTo("無效的確認碼，請重新判斷");
    }

    @Test @Order(2) void completeLatestLegalUnusedConfirmationAndRejectReplay() {
        var tokens=jdbc.queryForList("""
                SELECT CODE1 FROM CHECK_CONFIRMATION
                WHERE USED_AT IS NULL
                ORDER BY CREATED_AT DESC
                FETCH FIRST 1 ROWS ONLY
                """,String.class);
        Assumptions.assumeFalse(tokens.isEmpty(),"No unused confirmation: success Oracle E2E waits for a real WS1 token.");
        String token=tokens.get(0);
        var confirmation=repository.findConfirmation(token).orElseThrow();
        var exam=repository.findPatientExam(confirmation.patientId(),confirmation.examId()).orElseThrow();
        if (exam.status()==com.healthcheckassistant.patient.dto.ExamStatus.COMPLETED) {
            var conflict=http.postForEntity("/api/ws2/complete",Map.of("code1",token),JsonNode.class);
            assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(conflict.getBody().get("message").asText()).isEqualTo("此檢查已完成");
            assertThat(repository.findConfirmation(token).orElseThrow().usedAt()).isNull();
            Assumptions.abort("Latest unused token refers to a completed exam: conflict verified; success E2E still pending.");
        }
        var before=http.getForObject("/api/patients/{id}/exams",PatientProgressResponse.class,confirmation.patientId());
        var success=http.postForEntity("/api/ws2/complete",Map.of("code1",token),JsonNode.class);
        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(success.getBody().get("success").asBoolean()).isTrue();
        assertThat(success.getBody().get("message").asText()).isEqualTo("檢查完成狀態已更新");
        var completed=repository.findPatientExam(confirmation.patientId(),confirmation.examId()).orElseThrow();
        assertThat(completed.status()).isEqualTo(com.healthcheckassistant.patient.dto.ExamStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();
        assertThat(repository.findConfirmation(token).orElseThrow().usedAt()).isNotNull();
        var after=http.getForObject("/api/patients/{id}/exams",PatientProgressResponse.class,confirmation.patientId());
        assertThat(after.completedCount()).isEqualTo(before.completedCount()+1);
        assertThat(after.totalCount()).isEqualTo(before.totalCount());
        var replay=http.postForEntity("/api/ws2/complete",Map.of("code1",token),JsonNode.class);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(replay.getBody().get("message").asText()).isEqualTo("確認碼已使用，請勿重複提交");
    }
}
