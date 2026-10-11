package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.*;
import com.healthcheckassistant.common.error.ApiErrorResponse;
import com.healthcheckassistant.patient.PatientBasicData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/admin/patients")
@ApiResponse(responseCode = "400", description = "資料格式或長度不合法", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "404", description = "找不到受檢者", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "409", description = "病歷號重複或受檢者已有關聯紀錄", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "500", description = "服務暫時無法使用", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
public class PatientManagementController {
    private final PatientManagementService service;
    public PatientManagementController(PatientManagementService service) { this.service = service; }
    @GetMapping
    @Operation(summary = "受檢者資料列表（依病歷號排序）")
    @ApiResponse(responseCode = "200", description = "受檢者列表", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PatientBasicData.class))))
    public List<PatientBasicData> list() { return service.list(); }
    @GetMapping("/{patientId}")
    @Operation(summary = "查詢單筆受檢者資料")
    @ApiResponse(responseCode = "200", description = "受檢者與健檢安排", content = @Content(schema = @Schema(implementation = PatientDetailResponse.class)))
    public PatientDetailResponse get(@PathVariable @NotBlank @Size(max = 20) String patientId) { return service.get(patientId); }
    @PostMapping(consumes = "application/json")
    @Operation(summary = "新增受檢者與健檢安排（同一交易）")
    @ApiResponse(responseCode = "201", description = "新增成功", content = @Content(schema = @Schema(implementation = PatientDetailResponse.class)))
    public ResponseEntity<PatientDetailResponse> create(@Valid @RequestBody CreatePatientRequest request) {
        var patient = service.create(request);
        var uri = UriComponentsBuilder.fromPath("/api/admin/patients").pathSegment(patient.patientId()).build().encode().toUri();
        return ResponseEntity.created(uri).body(patient);
    }
    @PutMapping(value = "/{patientId}", consumes = "application/json")
    @Operation(summary = "修改姓名與健檢安排（不可修改病歷號與完成狀態）")
    @ApiResponse(responseCode = "200", description = "姓名與安排已更新", content = @Content(schema = @Schema(implementation = PatientDetailResponse.class)))
    public PatientDetailResponse update(@PathVariable @NotBlank @Size(max = 20) String patientId, @Valid @RequestBody UpdatePatientRequest request) {
        return service.update(patientId, request);
    }
    @DeleteMapping("/{patientId}")
    @Operation(summary = "刪除無關聯紀錄的受檢者")
    @ApiResponse(responseCode = "200", description = "受檢者已刪除", content = @Content(schema = @Schema(implementation = PatientDeleteResponse.class)))
    public PatientDeleteResponse delete(@PathVariable @NotBlank @Size(max = 20) String patientId) {
        service.delete(patientId);
        return new PatientDeleteResponse("受檢者已刪除");
    }
}
