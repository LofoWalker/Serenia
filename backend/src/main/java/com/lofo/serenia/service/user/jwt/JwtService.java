package com.lofo.serenia.service.user.jwt;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;

import java.time.Duration;

/**
 * Responsible for issuing JWTs for authenticated users.
 */
@AllArgsConstructor
@ApplicationScoped
public class JwtService {

    private final SereniaConfig sereniaConfig;
    private final UserMapper userMapper;

    /**
     * Builds a JWT from a user view DTO.
     */
    public String generateToken(UserResponseDTO user) {
        return buildToken(user);
    }
    
    private String buildToken(UserResponseDTO userView) {
        return Jwt.issuer(sereniaConfig.jwtIssuer())
                .upn(userView.email())
                .subject(userView.id().toString())
                .groups(userView.role())
                .expiresIn(Duration.ofSeconds(sereniaConfig.tokenExpirationTime()))
                .sign();
    }
}

