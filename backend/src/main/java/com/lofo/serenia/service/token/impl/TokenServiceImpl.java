package com.lofo.serenia.service.token.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.service.token.TokenService;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;

import java.time.Duration;

@AllArgsConstructor
@ApplicationScoped
public class TokenServiceImpl implements TokenService {

    private final SereniaConfig sereniaConfig;
    private final UserMapper userMapper;

    @Override
    public String generateToken(UserResponseDTO user) {
        return buildToken(user);
    }

    @Override
    public String generateToken(User user) {
        return buildToken(userMapper.toView(user));
    }

    private String buildToken(UserResponseDTO userView) {
        return Jwt.issuer(sereniaConfig.jwtIssuer())
                .upn(userView.email())
                .subject(userView.id().toString())
                .groups(userView.roles())
                .expiresIn(Duration.ofSeconds(sereniaConfig.tokenExpirationTime()))
                .sign();
    }
}
