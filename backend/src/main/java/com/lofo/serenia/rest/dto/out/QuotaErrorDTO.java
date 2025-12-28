package com.lofo.serenia.rest.dto.out;
/**
 * DTO pour les erreurs de quota dépassé.
 * Retourné avec un status HTTP 429.
 */
public record QuotaErrorDTO(
        String quotaType,
        int limit,
        int current,
        int requested,
        String message
) {
}
