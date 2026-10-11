package com.finance.tracker.access;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Corpo de erro no mesmo formato de {@code GlobalExceptionHandler}. */
final class AccessErrors {

    private AccessErrors() {
    }

    static Map<String, Object> body(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}
