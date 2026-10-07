package com.healthcheckassistant.ws1;

import com.healthcheckassistant.common.error.ApiErrorResponse;
import com.healthcheckassistant.ws1.dto.Ws1CheckRequest;
import com.healthcheckassistant.ws1.dto.Ws1CheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ws1")
public class Ws1Controller {
    private final Ws1Service service;

    public Ws1Controller(Ws1Service service) {
        this.service = service;
    }

    @PostMapping(value = "/check", consumes = "application/json")
    @Operation(summary = "WS1 進站判斷", description = "通過與正常業務拒絕均為 HTTP 200；允許時寫入 Oracle 確認碼，尚未完成檢查。")
    @ApiResponse(responseCode = "200", description = "allowed=true 或業務拒絕 allowed=false")
    @ApiResponse(responseCode = "400", description = "請求格式不正確",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "找不到受檢者或站點",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "伺服器處理失敗",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Ws1CheckResponse check(@Valid @RequestBody Ws1CheckRequest request) {
        return service.check(request);
    }
}
