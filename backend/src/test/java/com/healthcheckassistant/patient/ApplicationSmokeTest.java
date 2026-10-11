package com.healthcheckassistant.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Placeholder is only for application startup; repository mock prevents Oracle access.
@SpringBootTest(properties = "DB_PASSWORD=unused-test-placeholder")
@AutoConfigureMockMvc
class ApplicationSmokeTest {
 @Autowired MockMvc mvc;
 @MockitoBean PatientRepository repository;
 @MockitoBean com.healthcheckassistant.admin.AdminRepository adminRepository;
 @Test void oracleDriverLoads() throws Exception {
  assertThat(Class.forName("oracle.jdbc.OracleDriver")).isNotNull();
 }
 @Test void swaggerUiResourcesAreAvailable() throws Exception {
  mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
  mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/v3/api-docs"));
  verifyNoInteractions(repository);
 }
 @Test void openApiDescribesImplementedEndpointsWithErrorSchema() throws Exception {
  mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
   .andDo(result -> {
    String snapshot = System.getProperty("openapi.snapshot");
    if (snapshot != null) java.nio.file.Files.writeString(java.nio.file.Path.of(snapshot), result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.UTF_8);
   })
   .andExpect(jsonPath("$.paths.length()").value(8))
   .andExpect(jsonPath("$.paths['/api/admin/exams'].get.responses['200'].content['*/*'].schema.type").value("array"))
   .andExpect(jsonPath("$.paths['/api/admin/patients'].get").exists())
   .andExpect(jsonPath("$.paths['/api/admin/patients'].post.responses['201']").exists())
   .andExpect(jsonPath("$.paths['/api/admin/patients/{patientId}'].get").exists())
   .andExpect(jsonPath("$.paths['/api/admin/patients/{patientId}'].put").exists())
   .andExpect(jsonPath("$.paths['/api/admin/patients/{patientId}'].delete").exists())
   .andExpect(jsonPath("$.paths['/api/admin/patients'].get.responses['200'].content['*/*'].schema.type").value("array"))
   .andExpect(jsonPath("$.paths['/api/admin/patients/{patientId}'].get.responses['200'].content['*/*'].schema['$ref']").value("#/components/schemas/PatientDetailResponse"))
   .andExpect(jsonPath("$.paths['/api/admin/patients/{patientId}'].put.responses['200'].content['*/*'].schema['$ref']").value("#/components/schemas/PatientDetailResponse"))
   .andExpect(jsonPath("$.paths['/api/admin/patients/{patientId}'].delete.responses['200'].content['*/*'].schema['$ref']").value("#/components/schemas/PatientDeleteResponse"))
   .andExpect(jsonPath("$.paths['/api/admin/progress'].get.responses['200'].content['*/*'].schema.type").value("array"))
   .andExpect(jsonPath("$.paths['/api/admin/progress'].get.responses['200'].content['*/*'].schema.items['$ref']").value("#/components/schemas/AdminProgressResponse"))
   .andExpect(jsonPath("$.components.schemas.AdminProgressResponse.properties.length()").value(4))
   .andExpect(jsonPath("$.components.schemas.AdminProgressResponse.properties.completedCount.type").value("integer"))
   .andExpect(jsonPath("$.paths['/api/patients/{patientId}/next'].get.responses['200'].description").value(org.hamcrest.Matchers.containsString("nextExam=null")))
   .andExpect(jsonPath("$.paths['/api/patients/{patientId}/next'].get.responses['404'].content['*/*'].schema['$ref']").value("#/components/schemas/ApiErrorResponse"))
   .andExpect(jsonPath("$.paths['/api/patients/{patientId}/exams'].get.responses['200']").exists())
   .andExpect(jsonPath("$.paths['/api/patients/{patientId}/exams'].get.responses['404'].content['*/*'].schema['$ref']").value("#/components/schemas/ApiErrorResponse"))
   .andExpect(jsonPath("$.components.schemas.ExamResponse.properties.status.enum[0]").value("PENDING"))
   .andExpect(jsonPath("$.paths['/api/ws1/check'].post.responses['200']").exists())
   .andExpect(jsonPath("$.paths['/api/ws1/check'].post.responses['400']").exists())
   .andExpect(jsonPath("$.paths['/api/ws1/check'].post.responses['404']").exists())
   .andExpect(jsonPath("$.components.schemas.Ws1CheckRequest.properties.patientId.example").value("P001"))
   .andExpect(jsonPath("$.components.schemas.Ws1CheckRequest.properties.stationId.example").value("S002"))
   .andExpect(jsonPath("$.paths['/api/ws2/complete'].post.responses['200']").exists())
   .andExpect(jsonPath("$.paths['/api/ws2/complete'].post.responses['400']").exists())
   .andExpect(jsonPath("$.paths['/api/ws2/complete'].post.responses['404']").exists())
   .andExpect(jsonPath("$.paths['/api/ws2/complete'].post.responses['409'].content['*/*'].schema['$ref']").value("#/components/schemas/ApiErrorResponse"))
   .andExpect(jsonPath("$.components.schemas.Ws2CompleteRequest.properties.code1.example").exists());
  verifyNoInteractions(repository);
 }
 @Autowired java.time.Clock clock;
 @Test void productionClockUsesTaipeiTime() {
  assertThat(clock.getZone()).isEqualTo(java.time.ZoneId.of("Asia/Taipei"));
 }
 @Test void unknownRouteReturns404Message() throws Exception {
  mvc.perform(get("/api/unknown")).andExpect(status().isNotFound()).andExpect(content().json("{\"message\":\"找不到資源\"}",true));
  verifyNoInteractions(repository);
 }
 @Test void unsupportedMethodReturns405Message() throws Exception {
  mvc.perform(post("/api/patients/P001/exams")).andExpect(status().isMethodNotAllowed()).andExpect(content().json("{\"message\":\"不支援此 HTTP 方法\"}",true));
  verifyNoInteractions(repository);
 }
}

