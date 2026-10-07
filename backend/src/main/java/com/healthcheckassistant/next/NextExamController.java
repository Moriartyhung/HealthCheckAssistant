package com.healthcheckassistant.next;

import com.healthcheckassistant.common.error.ApiErrorResponse;
import com.healthcheckassistant.next.dto.NextExamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class NextExamController {
    private final NextExamService service;

    public NextExamController(NextExamService service) {
        this.service = service;
    }

    @GetMapping("/{patientId}/next")
    @Operation(summary = "依時段與前置條件推薦下一站")
    @ApiResponse(responseCode = "200", description = "建議前往以下站點（nextExam 為完整 Exam）；所有檢查已完成，謝謝您的配合！（nextExam=null）；目前沒有可進行的檢查，請洽工作人員（nextExam=null）")
    @ApiResponse(responseCode = "400", description = "參數不正確",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "找不到受檢者",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "服務暫時無法使用",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public NextExamResponse getNext(@PathVariable @NotBlank @Size(max = 20) String patientId) {
        return service.getNext(patientId);
    }
}
