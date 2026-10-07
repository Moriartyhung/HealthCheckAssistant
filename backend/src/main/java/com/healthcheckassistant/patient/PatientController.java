package com.healthcheckassistant.patient;

import com.healthcheckassistant.common.error.ApiErrorResponse;
import com.healthcheckassistant.patient.dto.PatientProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping("/{patientId}/exams")
    @Operation(summary = "取得受檢者健檢項目與完成進度")
    @ApiResponse(responseCode = "200", description = "健檢進度")
    @ApiResponse(responseCode = "400", description = "參數不正確",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "找不到受檢者",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "服務暫時無法使用",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public PatientProgressResponse getExams(
            @PathVariable @NotBlank @Size(max = 20) String patientId) {
        return service.getExams(patientId);
    }
}
