package com.lofo.serenia.persistence.entity.conversation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "messages")
@NoArgsConstructor
public class Message {

    /**
     * Persisted chat turn stored encrypted for privacy-compliant replay and audit operations.
     */
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "encrypted_content", nullable = false)
    private byte[] encryptedContent;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

}
