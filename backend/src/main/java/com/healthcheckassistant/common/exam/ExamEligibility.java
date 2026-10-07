package com.healthcheckassistant.common.exam;

import com.healthcheckassistant.patient.dto.ExamStatus;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.function.BooleanSupplier;

/** Shared WS1/Next rules; prerequisite lookup stays lazy after status and time checks. */
public final class ExamEligibility {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private ExamEligibility() {}

    public static LocalTime parseTime(String value) {
        return LocalTime.parse(value, TIME_FORMAT);
    }

    /** Null means eligible; otherwise preserves the WS1 denial message. */
    public static String denial(ExamStatus status, LocalTime start, LocalTime end,
                                LocalTime now, BooleanSupplier prerequisitesSatisfied) {
        if (status == ExamStatus.COMPLETED) {
            return "此檢查已完成，無需重複檢查";
        }
        if (now.isBefore(start) || now.isAfter(end)) {
            return "不在允許檢查時間（" + start.format(TIME_FORMAT) + "–" + end.format(TIME_FORMAT) + "）";
        }
        return prerequisitesSatisfied.getAsBoolean() ? null : "前置檢查尚未完成";
    }
}
