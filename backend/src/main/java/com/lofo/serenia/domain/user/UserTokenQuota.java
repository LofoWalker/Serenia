package com.lofo.serenia.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "user_token_quotas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class UserTokenQuota {

    /**
     * Tracks per-user token allowances and consumption counters to guard billing abuse.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Column(name = "input_tokens_limit", nullable = false)
    private Long inputTokensLimit;

    @Column(name = "output_tokens_limit", nullable = false)
    private Long outputTokensLimit;

    @Column(name = "total_tokens_limit", nullable = false)
    private Long totalTokensLimit;

    @Builder.Default
    @Column(name = "input_tokens_used", nullable = false)
    private Long inputTokensUsed = 0L;

    @Builder.Default
    @Column(name = "output_tokens_used", nullable = false)
    private Long outputTokensUsed = 0L;

    @Builder.Default
    @Column(name = "total_tokens_used", nullable = false)
    private Long totalTokensUsed = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Registers input token consumption and updates timestamps atomically for optimistic locking flows.
     */
    public void addInputTokens(long tokens) {
        this.inputTokensUsed += tokens;
        this.totalTokensUsed += tokens;
    }

    /**
     * Registers output token consumption.
     */
    public void addOutputTokens(long tokens) {
        this.outputTokensUsed += tokens;
        this.totalTokensUsed += tokens;
    }

    /**
     * Validates whether the next request fits within configured quotas.
     */
    public boolean canConsumeTokens(long inputTokens, long outputTokens) {
        return (inputTokensUsed + inputTokens <= inputTokensLimit)
                && (outputTokensUsed + outputTokens <= outputTokensLimit)
                && (totalTokensUsed + inputTokens + outputTokens <= totalTokensLimit);
    }

    public long getRemainingInputTokens() {
        return Math.max(0, inputTokensLimit - inputTokensUsed);
    }

    public long getRemainingOutputTokens() {
        return Math.max(0, outputTokensLimit - outputTokensUsed);
    }

    public long getRemainingTotalTokens() {
        return Math.max(0, totalTokensLimit - totalTokensUsed);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
