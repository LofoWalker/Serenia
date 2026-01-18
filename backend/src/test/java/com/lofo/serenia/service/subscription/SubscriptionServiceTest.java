package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.rest.dto.out.PlanDTO;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Unit Tests")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserFinder userFinder;

    private SubscriptionService subscriptionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(subscriptionRepository, planRepository, userFinder);
    }

    @Nested
    @DisplayName("createDefaultSubscription")
    class CreateDefaultSubscription {

        @Test
        @DisplayName("should create subscription with free plan")
        void should_create_subscription_with_free_plan() {
            User user = createUser();
            Plan freePlan = createPlan(PlanType.FREE);

            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userFinder.findByIdOrThrow(USER_ID)).thenReturn(user);
            when(planRepository.findByName(PlanType.FREE)).thenReturn(Optional.of(freePlan));

            Subscription subscription = subscriptionService.createDefaultSubscription(USER_ID);

            assertThat(subscription).isNotNull();
            assertThat(subscription.getPlan().getName()).isEqualTo(PlanType.FREE);
            verify(subscriptionRepository).persist(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("createSubscription")
    class CreateSubscription {

        @Test
        @DisplayName("should create subscription with specified plan")
        void should_create_subscription_with_specified_plan() {
            User user = createUser();
            Plan plusPlan = createPlan(PlanType.PLUS);

            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userFinder.findByIdOrThrow(USER_ID)).thenReturn(user);
            when(planRepository.findByName(PlanType.PLUS)).thenReturn(Optional.of(plusPlan));

            Subscription subscription = subscriptionService.createSubscription(USER_ID, PlanType.PLUS);

            assertThat(subscription).isNotNull();
            assertThat(subscription.getPlan().getName()).isEqualTo(PlanType.PLUS);
            assertThat(subscription.getTokensUsedThisMonth()).isZero();
            assertThat(subscription.getMessagesSentToday()).isZero();
        }

        @Test
        @DisplayName("should throw when user already has subscription")
        void should_throw_when_user_already_has_subscription() {
            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> subscriptionService.createSubscription(USER_ID, PlanType.FREE))
                    .isInstanceOf(SereniaException.class)
                    .hasMessageContaining("User already has a subscription");
        }

        @Test
        @DisplayName("should throw when user not found")
        void should_throw_when_user_not_found() {
            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userFinder.findByIdOrThrow(USER_ID)).thenThrow(SereniaException.notFound("User not found"));

            assertThatThrownBy(() -> subscriptionService.createSubscription(USER_ID, PlanType.FREE))
                    .isInstanceOf(SereniaException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("getSubscription")
    class GetSubscription {

        @Test
        @DisplayName("should return subscription for user")
        void should_return_subscription_for_user() {
            Subscription expectedSubscription = createSubscription();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(expectedSubscription));

            Subscription subscription = subscriptionService.getSubscription(USER_ID);

            assertThat(subscription).isNotNull();
            assertThat(subscription.getUser().getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw when subscription not found")
        void should_throw_when_subscription_not_found() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.getSubscription(USER_ID))
                    .isInstanceOf(SereniaException.class)
                    .hasMessageContaining("Subscription not found");
        }
    }

    @Nested
    @DisplayName("changePlan")
    class ChangePlan {

        @Test
        @DisplayName("should change plan successfully")
        void should_change_plan_successfully() {
            Subscription subscription = createSubscription();
            Plan maxPlan = createPlan(PlanType.MAX);

            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));
            when(planRepository.findByName(PlanType.MAX)).thenReturn(Optional.of(maxPlan));

            Subscription updated = subscriptionService.changePlan(USER_ID, PlanType.MAX);

            assertThat(updated.getPlan().getName()).isEqualTo(PlanType.MAX);
            verify(subscriptionRepository).persist(subscription);
        }

        @Test
        @DisplayName("should throw when plan not found")
        void should_throw_when_plan_not_found() {
            Subscription subscription = createSubscription();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));
            when(planRepository.findByName(PlanType.MAX)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.changePlan(USER_ID, PlanType.MAX))
                    .isInstanceOf(SereniaException.class)
                    .hasMessageContaining("Plan not found");
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return correct status dto")
        void should_return_correct_status_dto() {
            Subscription subscription = createSubscription();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));

            SubscriptionStatusDTO status = subscriptionService.getStatus(USER_ID);

            assertThat(status).isNotNull();
            assertThat(status.planName()).isEqualTo(PlanType.FREE.name());
        }
    }

    @Nested
    @DisplayName("getAllPlans")
    class GetAllPlans {

        @Test
        @DisplayName("should return all available plans")
        void should_return_all_available_plans() {
            List<Plan> plans = List.of(
                    createPlan(PlanType.FREE),
                    createPlan(PlanType.PLUS),
                    createPlan(PlanType.MAX)
            );
            when(planRepository.listAll()).thenReturn(plans);

            List<PlanDTO> result = subscriptionService.getAllPlans();

            assertThat(result).hasSize(3);
        }
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.USER);
        return user;
    }

    private Plan createPlan(PlanType type) {
        Plan plan = new Plan();
        plan.setId(UUID.randomUUID());
        plan.setName(type);
        plan.setMonthlyTokenLimit(10000);
        plan.setDailyMessageLimit(10);
        plan.setPriceCents(type == PlanType.FREE ? 0 : type == PlanType.PLUS ? 999 : 1999);
        plan.setCurrency("EUR");
        return plan;
    }

    private Subscription createSubscription() {
        User user = createUser();
        Plan plan = createPlan(PlanType.FREE);

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setTokensUsedThisMonth(0);
        subscription.setMessagesSentToday(0);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setMonthlyPeriodStart(LocalDateTime.now());
        subscription.setDailyPeriodStart(LocalDateTime.now());
        return subscription;
    }
}
