package com.lofo.serenia.dto.out;

/**
 * Returned when login succeeds, carrying the granted JWT and hydrated profile.
 */
public record AuthResponseDTO(UserResponseDTO user, String token) {
}
