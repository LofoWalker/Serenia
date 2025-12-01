package com.lofo.serenia.service.token.impl;

import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.service.token.TokenService;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class TokenServiceImpl implements TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;
    @ConfigProperty(name = "serenia.auth.expiration-time")
    Long duration;

    public String generateToken(UserResponseDTO user) {
        return buildToken(user);
    }

    public String generateToken(User user) {
        return buildToken(toView(user));
    }

    private String buildToken(UserResponseDTO userView) {
        return Jwt.issuer(issuer)
                .upn(userView.email())
                .subject(userView.id().toString())
                .groups(userView.roles())
                .expiresIn(Duration.ofSeconds(duration))
                .sign();
    }

    private UserResponseDTO toView(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserResponseDTO(user.getId(), user.getLastName(), user.getFirstName(), user.getEmail(), roles);
    }
}
