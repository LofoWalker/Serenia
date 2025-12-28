package com.lofo.serenia.persistence.entity.subscription;
import com.lofo.serenia.persistence.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
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
    private LocalDateTime monthlyPeriodStart;
    @Column(name = "daily_period_start", nullable = false)
    private LocalDateTime dailyPeriodStart;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    /**
     * Vérifie si la période mensuelle est expirée.
     */
    public boolean isMonthlyPeriodExpired() {
        return LocalDateTime.now().isAfter(monthlyPeriodStart.plusMonths(1));
    }
    /**
     * Vérifie si la période journalière est expirée.
     */
    public boolean isDailyPeriodExpired() {
        return LocalDateTime.now().isAfter(dailyPeriodStart.plusDays(1));
    }
    /**
     * Réinitialise le compteur mensuel et met à jour la période.
     */
    public void resetMonthlyPeriod() {
        this.tokensUsedThisMonth = 0;
        this.monthlyPeriodStart = LocalDateTime.now();
    }
    /**
     * Réinitialise le compteur journalier et met à jour la période.
     */
    public void resetDailyPeriod() {
        this.messagesSentToday = 0;
        this.dailyPeriodStart = LocalDateTime.now();
    }
}
