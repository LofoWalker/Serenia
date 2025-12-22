package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.user.BaseToken;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BaseTokenRepository implements PanacheRepository<BaseToken> {

    public Optional<BaseToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public void deleteByUserId(UUID userId) {
        delete("user.id", userId);
    }

    public void deleteByToken(String token) {
        delete("token", token);
    }
}

