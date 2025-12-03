package com.lofo.serenia.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_token_usages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class UserTokenUsage {

    /**
     * Immutable audit entry for each chargeable interaction (one row per completion request).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "input_tokens", nullable = false)
    private Long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Long outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens;

    @Column(name = "usage_type", nullable = false, length = 50)
    private String usageType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
