package com.razorrecover.dto;

import java.time.Instant;
import java.util.Map;

/** A stable error envelope for future REST endpoints. */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors) {
}
