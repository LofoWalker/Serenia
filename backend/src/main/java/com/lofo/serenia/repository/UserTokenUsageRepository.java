package com.lofo.serenia.repository;

import com.lofo.serenia.domain.user.UserTokenUsage;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserTokenUsageRepository implements PanacheRepository<UserTokenUsage> {

    /**
     * Retrieves usage records for billing windows between the supplied dates (inclusive).
     */
    public List<UserTokenUsage> findByUserIdAndDateRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return find("user.id = ?1 AND createdAt >= ?2 AND createdAt <= ?3", userId, startDate, endDate)
                .list();
    }

    public List<UserTokenUsage> findByUserIdAndUsageType(UUID userId, String usageType) {
        return find("user.id = ?1 AND usageType = ?2", userId, usageType)
                .list();
    }

    public void deleteByUserId(UUID userId) {
        delete("user.id", userId);
    }
}
