package com.healthcheckassistant.ws2;

import com.healthcheckassistant.common.error.ApiErrorResponse;
import com.healthcheckassistant.ws2.dto.Ws2CompleteRequest;
import com.healthcheckassistant.ws2.dto.Ws2CompleteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ws2")
public class Ws2Controller {
    private final Ws2Service service;

    public Ws2Controller(Ws2Service service) {
        this.service = service;
    }

    @PostMapping(value = "/complete", consumes = "application/json")
    @Operation(summary = "WS2 完成檢查", description = "同一交易內消耗確認碼並完成檢查；不重新判斷 WS1 時段，確認碼沒有到期機制。")
    @ApiResponse(responseCode = "200", description = "檢查完成狀態已更新")
    @ApiResponse(responseCode = "400", description = "請提供 code1",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "無效的確認碼",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "確認碼已使用或此檢查已完成",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "服務暫時無法使用",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Ws2CompleteResponse complete(@Valid @RequestBody Ws2CompleteRequest request) {
        return service.complete(request);
    }
}
