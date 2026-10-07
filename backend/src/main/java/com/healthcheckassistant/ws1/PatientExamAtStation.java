package com.healthcheckassistant.ws1;

import com.healthcheckassistant.patient.dto.ExamStatus;
import java.time.LocalTime;

public record PatientExamAtStation(String examId, String examName, ExamStatus status,
                                   LocalTime startTime, LocalTime endTime) {}
