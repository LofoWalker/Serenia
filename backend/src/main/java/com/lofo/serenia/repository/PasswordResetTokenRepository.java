package com.lofo.serenia.repository;

import com.lofo.serenia.domain.user.PasswordResetToken;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PasswordResetTokenRepository implements PanacheRepository<PasswordResetToken> {

    public Optional<PasswordResetToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public void deleteByUserId(UUID userId) {
        delete("user.id", userId);
    }

    public void deleteByToken(String token) {
        delete("token", token);
    }
}

