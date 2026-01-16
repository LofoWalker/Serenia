package com.lofo.serenia.persistence.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Temporary token used for sensitive operations (account activation, password reset, etc.).
 * The endpoint/service determines the action to perform based on the usage context.
 * Each token is linked to a user and has an expiration date.
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
     * Checks if this token has expired.
     *
     * @return true if the token has expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
}

