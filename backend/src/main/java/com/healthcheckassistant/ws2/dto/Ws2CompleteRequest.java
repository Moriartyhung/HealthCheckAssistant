package com.healthcheckassistant.ws2.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.healthcheckassistant.common.json.RequestStringDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Ws2CompleteRequest(
        @NotBlank @Size(max = 100)
        @Schema(example = "2b0612e0-3d39-4c35-a117-5a8a60c0e619")
        @JsonDeserialize(using = RequestStringDeserializer.class) String code1) {}
