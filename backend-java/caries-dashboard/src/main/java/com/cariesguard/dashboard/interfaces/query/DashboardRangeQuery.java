package com.cariesguard.dashboard.interfaces.query;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import java.time.LocalDate;

public record DashboardRangeQuery(
        String rangeType,
        LocalDate startDate,
        LocalDate endDate) {

    public static DashboardRangeQuery of(String rangeType, LocalDate startDate, LocalDate endDate) {
        String normalized = normalize(rangeType);
        LocalDate today = LocalDate.now();
        return switch (normalized) {
            case "TODAY" -> new DashboardRangeQuery(normalized, today, today);
            case "LAST_7_DAYS" -> new DashboardRangeQuery(normalized, today.minusDays(6), today);
            case "LAST_30_DAYS" -> new DashboardRangeQuery(normalized, today.minusDays(29), today);
            case "CUSTOM" -> buildCustomRange(startDate, endDate);
            default -> throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(),
                    "Unsupported dashboard rangeType: " + normalized);
        };
    }

    private static DashboardRangeQuery buildCustomRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(),
                    "CUSTOM rangeType requires both startDate and endDate");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(),
                    "startDate must not be after endDate");
        }
        return new DashboardRangeQuery("CUSTOM", startDate, endDate);
    }

    private static String normalize(String rangeType) {
        if (rangeType == null || rangeType.isBlank()) {
            return "LAST_7_DAYS";
        }
        return rangeType.trim().toUpperCase();
    }
}
