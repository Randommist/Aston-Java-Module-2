package org.example.userservice.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors
) {
}
