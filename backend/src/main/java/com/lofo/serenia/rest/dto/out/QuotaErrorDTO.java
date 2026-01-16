package com.lofo.serenia.rest.dto.out;
/**
 * DTO for quota exceeded errors.
 * Returned with HTTP 429 status.
 */
public record QuotaErrorDTO(
        String quotaType,
        int limit,
        int current,
        int requested,
        String message
) {
}
