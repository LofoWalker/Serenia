package com.lofo.serenia.repository;

import com.lofo.serenia.domain.user.UserTokenQuota;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserTokenQuotaRepository implements PanacheRepository<UserTokenQuota> {

    public Optional<UserTokenQuota> findByUserId(UUID userId) {
        return find("user.id", userId).firstResultOptional();
    }

    public void deleteByUserId(UUID userId) {
        delete("user.id", userId);
    }
}

