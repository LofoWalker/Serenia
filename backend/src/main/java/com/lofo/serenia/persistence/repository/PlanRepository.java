package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
/**
 * Repository for subscription plan access.
 */
@ApplicationScoped
public class PlanRepository implements PanacheRepository<Plan> {
    /**
     * Find a plan by its type.
     */
    public Optional<Plan> findByName(PlanType planType) {
        return find("name", planType).firstResultOptional();
    }

    /**
     * Get the default FREE plan.
     */
    public Plan getFreePlan() {
        return findByName(PlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("Plan FREE not found in database"));
    }
}
