package com.lofo.serenia.persistence.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Token temporaire utilisé pour les opérations sensibles (activation de compte, réinitialisation de mot de passe, etc.).
 * L'endpoint/service détermine l'action à effectuer en fonction du contexte d'utilisation.
 * Chaque token est lié à un utilisateur et possède une date d'expiration.
 */
@Entity
@Table(name = "tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class BaseToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Vérifie si ce token a expiré.
     *
     * @return true si le token a expiré, false sinon
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
}

