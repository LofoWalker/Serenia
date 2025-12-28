package com.lofo.serenia.persistence.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entité représentant un utilisateur de l'application.
 * Stocke les informations de profil de base et l'état d'activation du compte.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Builder.Default
    @Column(name = "is_account_activated", nullable = false)
    private boolean accountActivated = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role = Role.USER;
}
