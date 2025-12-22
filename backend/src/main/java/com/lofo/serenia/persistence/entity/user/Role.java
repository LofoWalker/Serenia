package com.lofo.serenia.persistence.entity.user;

/**
 * Rôle utilisateur pour contrôler l'accès et les permissions dans l'application.
 * Stocké directement dans la colonne 'role' de la table users.
 */
public enum Role {
    /**
     * Rôle utilisateur standard avec accès limité aux ressources personnelles.
     */
    USER,

    /**
     * Rôle administrateur avec accès complet à toutes les ressources et fonctionnalités.
     */
    ADMIN
}
