package com.lofo.serenia.service.token;

import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.out.UserResponseDTO;

/**
 * Responsible for issuing JWTs for authenticated users.
 */
public interface TokenService {

    /**
     * Builds a JWT from a user view DTO.
     */
    String generateToken(UserResponseDTO user);

    /**
     * Builds a JWT directly from a user entity.
     */
    String generateToken(User user);
}
