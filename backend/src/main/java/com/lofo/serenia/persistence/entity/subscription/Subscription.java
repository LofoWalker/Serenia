package com.lofo.serenia.persistence.entity.subscription;

import com.lofo.serenia.persistence.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité représentant l'abonnement d'un utilisateur.
 * Contient l'état de consommation (compteurs) et les périodes de reset.
 * Relation 1:1 avec User.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "plan"})
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Builder.Default
    @Column(name = "tokens_used_this_month", nullable = false)
    private Integer tokensUsedThisMonth = 0;

    @Builder.Default
    @Column(name = "messages_sent_today", nullable = false)
    private Integer messagesSentToday = 0;

    @Column(name = "monthly_period_start", nullable = false)
    private Instant monthlyPeriodStart;

    @Column(name = "daily_period_start", nullable = false)
    private Instant dailyPeriodStart;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 32)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Builder.Default
    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd = false;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Vérifie si la période mensuelle est expirée.
     * Uses UTC-based month calculation via ZonedDateTime to handle variable-length months correctly.
     */
    public boolean isMonthlyPeriodExpired() {
        return Instant.now().isAfter(
            monthlyPeriodStart.atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant()
        );
    }

    /**
     * Vérifie si la période journalière est expirée.
     */
    public boolean isDailyPeriodExpired() {
        return Instant.now().isAfter(
            dailyPeriodStart.atZone(java.time.ZoneOffset.UTC).plusDays(1).toInstant()
        );
    }

    /**
     * Réinitialise le compteur mensuel et met à jour la période.
     */
    public void resetMonthlyPeriod() {
        this.tokensUsedThisMonth = 0;
        this.monthlyPeriodStart = Instant.now();
    }

    /**
     * Réinitialise le compteur journalier et met à jour la période.
     */
    public void resetDailyPeriod() {
        this.messagesSentToday = 0;
        this.dailyPeriodStart = Instant.now();
    }
}
