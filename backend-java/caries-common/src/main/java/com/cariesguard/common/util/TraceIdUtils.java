package com.cariesguard.common.util;

import com.cariesguard.common.constant.TraceIdConstants;
import java.util.UUID;
import org.slf4j.MDC;

public final class TraceIdUtils {

    private TraceIdUtils() {
    }

    public static String currentTraceId() {
        return MDC.get(TraceIdConstants.TRACE_ID_MDC_KEY);
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
