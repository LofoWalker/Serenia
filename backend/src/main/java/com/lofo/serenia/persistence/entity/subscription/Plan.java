package com.lofo.serenia.persistence.entity.subscription;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * Entité représentant un plan d'abonnement.
 * Définit les limites d'utilisation pour tous les utilisateurs du plan.
 */
@Entity
@Table(name = "plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 32)
    private PlanType name;


    @Column(name = "monthly_token_limit", nullable = false)
    private Integer monthlyTokenLimit;

    @Column(name = "daily_message_limit", nullable = false)
    private Integer dailyMessageLimit;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "stripe_price_id", length = 255)
    private String stripePriceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
