package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.AdminProgressResponse;
import com.healthcheckassistant.common.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/progress")
    @Operation(summary = "查詢受檢者完成進度")
    @ApiResponse(responseCode = "200", description = "Current patient progress list (empty array when no patients)")
    @ApiResponse(responseCode = "500", description = "服務暫時無法使用",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public List<AdminProgressResponse> getProgress() {
        return service.getProgress();
    }
}
