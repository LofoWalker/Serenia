# Phase 1 : Modèle de données et Migration

**Durée estimée:** 2-3h

---

## 1.1 Migration Liquibase

### Fichier à créer

`src/main/resources/db/changelog/02-plans-subscriptions.yaml`

### Structure de la migration

```yaml
databaseChangeLog:
  - changeSet:
      id: 02-plans-subscriptions
      author: [votre-nom]
      changes:
        # ==========================================
        # Table: plans
        # ==========================================
        - createTable:
            tableName: plans
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: varchar(32)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: per_message_token_limit
                  type: INTEGER
                  constraints:
                    nullable: false
              - column:
                  name: monthly_token_limit
                  type: INTEGER
                  constraints:
                    nullable: false
              - column:
                  name: daily_message_limit
                  type: INTEGER
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP WITH TIME ZONE
                  constraints:
                    nullable: false
              - column:
                  name: updated_at
                  type: TIMESTAMP WITH TIME ZONE
                  constraints:
                    nullable: false

        # ==========================================
        # Table: subscriptions
        # ==========================================
        - createTable:
            tableName: subscriptions
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: user_id
                  type: UUID
                  constraints:
                    nullable: false
                    unique: true  # Relation 1:1 avec users
              - column:
                  name: plan_id
                  type: UUID
                  constraints:
                    nullable: false
              - column:
                  name: tokens_used_this_month
                  type: INTEGER
                  constraints:
                    nullable: false
                  defaultValueNumeric: 0
              - column:
                  name: messages_sent_today
                  type: INTEGER
                  constraints:
                    nullable: false
                  defaultValueNumeric: 0
              - column:
                  name: monthly_period_start
                  type: TIMESTAMP WITH TIME ZONE
                  constraints:
                    nullable: false
              - column:
                  name: daily_period_start
                  type: TIMESTAMP WITH TIME ZONE
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP WITH TIME ZONE
                  constraints:
                    nullable: false
              - column:
                  name: updated_at
                  type: TIMESTAMP WITH TIME ZONE
                  constraints:
                    nullable: false

        # ==========================================
        # Foreign Keys
        # ==========================================
        - addForeignKeyConstraint:
            constraintName: fk_subscriptions_user
            baseTableName: subscriptions
            baseColumnNames: user_id
            referencedTableName: users
            referencedColumnNames: id
            onDelete: CASCADE

        - addForeignKeyConstraint:
            constraintName: fk_subscriptions_plan
            baseTableName: subscriptions
            baseColumnNames: plan_id
            referencedTableName: plans
            referencedColumnNames: id
            onDelete: RESTRICT

        # ==========================================
        # Indexes
        # ==========================================
        - createIndex:
            indexName: idx_subscriptions_user_id
            tableName: subscriptions
            columns:
              - column:
                  name: user_id

        - createIndex:
            indexName: idx_subscriptions_plan_id
            tableName: subscriptions
            columns:
              - column:
                  name: plan_id

        # ==========================================
        # Seed Data: Plans par défaut
        # ==========================================
        - insert:
            tableName: plans
            columns:
              - column:
                  name: id
                  valueComputed: gen_random_uuid()
              - column:
                  name: name
                  value: FREE
              - column:
                  name: per_message_token_limit
                  valueNumeric: 1000
              - column:
                  name: monthly_token_limit
                  valueNumeric: 10000
              - column:
                  name: daily_message_limit
                  valueNumeric: 10
              - column:
                  name: created_at
                  valueComputed: NOW()
              - column:
                  name: updated_at
                  valueComputed: NOW()

        - insert:
            tableName: plans
            columns:
              - column:
                  name: id
                  valueComputed: gen_random_uuid()
              - column:
                  name: name
                  value: PLUS
              - column:
                  name: per_message_token_limit
                  valueNumeric: 4000
              - column:
                  name: monthly_token_limit
                  valueNumeric: 100000
              - column:
                  name: daily_message_limit
                  valueNumeric: 50
              - column:
                  name: created_at
                  valueComputed: NOW()
              - column:
                  name: updated_at
                  valueComputed: NOW()

        - insert:
            tableName: plans
            columns:
              - column:
                  name: id
                  valueComputed: gen_random_uuid()
              - column:
                  name: name
                  value: MAX
              - column:
                  name: per_message_token_limit
                  valueNumeric: 8000
              - column:
                  name: monthly_token_limit
                  valueNumeric: 500000
              - column:
                  name: daily_message_limit
                  valueNumeric: 200
              - column:
                  name: created_at
                  valueComputed: NOW()
              - column:
                  name: updated_at
                  valueComputed: NOW()
```

### Modification du changelog.xml

Ajouter dans `src/main/resources/db/changelog/changelog.xml` :

```xml
<include file="db/changelog/02-plans-subscriptions.yaml"/>
```

---

## 1.2 Entités JPA

### 1.2.1 Enum PlanType

**Fichier:** `src/main/java/com/lofo/serenia/persistence/entity/subscription/PlanType.java`

```java
package com.lofo.serenia.persistence.entity.subscription;

/**
 * Types de plans disponibles dans l'application.
 */
public enum PlanType {
    FREE,
    PLUS,
    MAX
}
```

---

### 1.2.2 Entité Plan

**Fichier:** `src/main/java/com/lofo/serenia/persistence/entity/subscription/Plan.java`

```java
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

    @Column(name = "per_message_token_limit", nullable = false)
    private Integer perMessageTokenLimit;

    @Column(name = "monthly_token_limit", nullable = false)
    private Integer monthlyTokenLimit;

    @Column(name = "daily_message_limit", nullable = false)
    private Integer dailyMessageLimit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

---

### 1.2.3 Entité Subscription

**Fichier:** `src/main/java/com/lofo/serenia/persistence/entity/subscription/Subscription.java`

```java
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
```

---

## 1.3 Tâches

- [ ] Créer le dossier `src/main/java/com/lofo/serenia/persistence/entity/subscription/`
- [ ] Créer `PlanType.java`
- [ ] Créer `Plan.java`
- [ ] Créer `Subscription.java`
- [ ] Créer `02-plans-subscriptions.yaml`
- [ ] Modifier `changelog.xml` pour inclure la nouvelle migration
- [ ] Exécuter la migration et vérifier en base de données
- [ ] Vérifier que les 3 plans (FREE, PLUS, MAX) sont créés

---

## 1.4 Validation

```bash
# Démarrer la base de données
docker-compose up -d db

# Exécuter les migrations
./mvnw quarkus:dev

# Vérifier les tables créées
psql -h localhost -U serenia -d serenia -c "\dt"
psql -h localhost -U serenia -d serenia -c "SELECT * FROM plans;"
```

---

[← Retour au README](./README.md) | [Phase suivante : Repositories →](./02-repositories.md)

