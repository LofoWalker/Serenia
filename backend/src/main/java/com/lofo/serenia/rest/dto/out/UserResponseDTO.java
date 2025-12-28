package com.lofo.serenia.rest.dto.out;

import java.util.Set;
import java.util.UUID;

/**
 * Read model exposed to clients, omitting sensitive fields like passwords or tokens.
 */
public record UserResponseDTO(
        UUID id,
        String lastName,
        String firstName,
        String email,
        String role
) {
}
