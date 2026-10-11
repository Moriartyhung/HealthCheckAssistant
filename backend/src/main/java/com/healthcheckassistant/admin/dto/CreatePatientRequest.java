package com.healthcheckassistant.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.healthcheckassistant.common.json.RequestStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.List;

public record CreatePatientRequest(
        @NotBlank @Size(max = 20) @Schema(example = "P900")
        @JsonDeserialize(using = RequestStringDeserializer.class) String patientId,
        @NotBlank @Size(max = 100) @Schema(example = "王小明")
        @JsonDeserialize(using = RequestStringDeserializer.class) String patientName,
        @Size(max = 1000)
        @JsonDeserialize(contentUsing = RequestStringDeserializer.class)
        @Schema(description = "健檢項目代碼，未提供則不安排項目")
        List<@NotBlank @Size(max = 10) String> examIds) {
    @JsonAnySetter public void rejectOtherFields(String key, Object value) {
        throw new IllegalArgumentException("Only patientId, patientName and examIds may be provided");
    }
}
