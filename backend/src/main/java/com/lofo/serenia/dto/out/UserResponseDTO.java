package com.lofo.serenia.dto.out;

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
        Set<String> roles
) {
}
