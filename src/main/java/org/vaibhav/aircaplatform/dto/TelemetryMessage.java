package org.vaibhav.aircaplatform.dto;

public record TelemetryMessage(
        String service,
        String timestamp,
        String traceId,
        String exception,
        String message
) {}
