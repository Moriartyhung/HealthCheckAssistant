package com.healthcheckassistant.admin.dto;
import java.util.List;
public record ExamCatalogItem(String examId, String examName, String stationId, String stationName,
                              String startTime, String endTime, List<String> requiredExamIds) {}
