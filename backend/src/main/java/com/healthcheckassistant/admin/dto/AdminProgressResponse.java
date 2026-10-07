package com.healthcheckassistant.admin.dto;

public record AdminProgressResponse(
        String patientId,
        String patientName,
        int completedCount,
        int totalCount
) {}
