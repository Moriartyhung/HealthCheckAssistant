package com.healthcheckassistant.ws1.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.healthcheckassistant.common.json.RequestStringDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Ws1CheckRequest(
        @NotBlank @Size(max = 20) @Schema(example = "P001")
        @JsonDeserialize(using = RequestStringDeserializer.class) String patientId,
        @NotBlank @Size(max = 10) @Schema(example = "S002")
        @JsonDeserialize(using = RequestStringDeserializer.class) String stationId) {}
