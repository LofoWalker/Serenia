package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
/**
 * Repository pour l'accès aux plans d'abonnement.
 */
@ApplicationScoped
public class PlanRepository implements PanacheRepository<Plan> {
    /**
     * Recherche un plan par son type.
     */
    public Optional<Plan> findByName(PlanType planType) {
        return find("name", planType).firstResultOptional();
    }
    /**
     * Recherche un plan par son nom (String).
     */
    public Optional<Plan> findByName(String name) {
        return find("name", PlanType.valueOf(name)).firstResultOptional();
    }
    /**
     * Récupère le plan FREE par défaut.
     */
    public Plan getFreePlan() {
        return findByName(PlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("Plan FREE not found in database"));
    }
}
