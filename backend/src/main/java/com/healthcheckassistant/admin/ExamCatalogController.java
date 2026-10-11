package com.healthcheckassistant.admin;
import com.healthcheckassistant.admin.dto.ExamCatalogItem;
import com.healthcheckassistant.common.error.ApiErrorResponse;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;

@RestController
@RequestMapping("/api/admin/exams")
public class ExamCatalogController {
    private final PatientManagementService service;
    public ExamCatalogController(PatientManagementService service) { this.service = service; }
    @GetMapping
    @Operation(summary = "健檢項目選擇清單（含站點與直接前置項目）")
    @ApiResponse(responseCode = "200", description = "既有檢查項目", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExamCatalogItem.class))))
    @ApiResponse(responseCode = "500", description = "服務暫時無法使用", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public List<ExamCatalogItem> list() { return service.catalog(); }
}
