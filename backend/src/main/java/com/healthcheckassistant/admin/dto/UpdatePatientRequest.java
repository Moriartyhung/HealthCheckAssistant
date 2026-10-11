package com.healthcheckassistant.admin.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.healthcheckassistant.common.json.RequestStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UpdatePatientRequest(
        @NotBlank @Size(max = 100) @Schema(example = "王小明")
        @JsonDeserialize(using = RequestStringDeserializer.class) String patientName,
        @Size(max = 1000)
        @JsonDeserialize(contentUsing = RequestStringDeserializer.class)
        @Schema(description = "完整目標安排清單；未提供則保留現有安排")
        List<@NotBlank @Size(max = 10) String> examIds) {
    @JsonAnySetter
    public void rejectOtherFields(String key, Object value) {
        throw new IllegalArgumentException("Only patientName and examIds may be updated");
    }
}
