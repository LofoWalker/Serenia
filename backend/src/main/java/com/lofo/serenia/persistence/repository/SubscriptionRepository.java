package com.lofo.serenia.persistence.repository;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
/**
 * Repository pour l'accès aux subscriptions utilisateurs.
 */
@ApplicationScoped
public class SubscriptionRepository implements PanacheRepository<Subscription> {
    public Optional<Subscription> findByUserId(UUID userId) {
        return find("user.id", userId).firstResultOptional();
    }
    public Optional<Subscription> findByUserIdForUpdate(UUID userId) {
        return find("user.id", userId)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }
    @Transactional
    public int incrementUsage(UUID subscriptionId, int tokens) {
        return update("tokensUsedThisMonth = tokensUsedThisMonth + ?1, " +
                        "messagesSentToday = messagesSentToday + 1, " +
                        "updatedAt = ?2 WHERE id = ?3",
                tokens, LocalDateTime.now(), subscriptionId);
    }
    public boolean existsByUserId(UUID userId) {
        return count("user.id", userId) > 0;
    }
}
