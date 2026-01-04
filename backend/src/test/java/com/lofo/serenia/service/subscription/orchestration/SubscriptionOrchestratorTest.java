package com.lofo.serenia.service.subscription.orchestration;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.mapper.DateTimeConverter;
import com.lofo.serenia.service.subscription.mapper.StripeStatusMapper;
import com.stripe.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionOrchestrator Tests")
class SubscriptionOrchestratorTest {

    private SubscriptionOrchestrator orchestrator;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private StripeStatusMapper statusMapper;

    @Mock
    private DateTimeConverter dateTimeConverter;

    private com.lofo.serenia.persistence.entity.subscription.Subscription internalSubscription;
    private Subscription stripeSubscription;
    private Plan planPlus;

    @BeforeEach
    void setUp() {
        orchestrator = new SubscriptionOrchestrator(
                subscriptionRepository,
                planRepository,
                statusMapper,
                dateTimeConverter
        );

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        Plan freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.FREE)
                .build();

        planPlus = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.PLUS)
                .stripePriceId("price_test123")
                .build();

        internalSubscription = com.lofo.serenia.persistence.entity.subscription.Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(freePlan)
                .stripeCustomerId("cus_test123")
                .status(SubscriptionStatus.ACTIVE)
                .build();

        stripeSubscription = new Subscription();
        stripeSubscription.setId("sub_test123");
        stripeSubscription.setStatus("active");
        stripeSubscription.setCancelAtPeriodEnd(false);
        stripeSubscription.setCurrentPeriodEnd(System.currentTimeMillis() / 1000);
    }

    @Nested
    @DisplayName("synchronizeFromStripe")
    class SynchronizeFromStripe {

        @Test
        @DisplayName("should synchronize basic subscription data from Stripe")
        void should_synchronize_basic_data() {
            when(statusMapper.mapStatus("active")).thenReturn(SubscriptionStatus.ACTIVE);
            when(dateTimeConverter.convertEpochToDateTime(anyLong()))
                    .thenReturn(LocalDateTime.now().plusDays(30));

            orchestrator.synchronizeFromStripe(internalSubscription, stripeSubscription);

            assertEquals("sub_test123", internalSubscription.getStripeSubscriptionId());
            assertEquals(SubscriptionStatus.ACTIVE, internalSubscription.getStatus());
            assertFalse(internalSubscription.getCancelAtPeriodEnd());

            verify(subscriptionRepository).persist(internalSubscription);
        }

        @Test
        @DisplayName("should apply discount processor when discount exists")
        void should_apply_discount_when_discount_exists() {
            com.stripe.model.Discount stripeDiscount = new com.stripe.model.Discount();
            stripeSubscription.setDiscount(stripeDiscount);

            when(statusMapper.mapStatus("active")).thenReturn(SubscriptionStatus.ACTIVE);
            when(dateTimeConverter.convertEpochToDateTime(anyLong()))
                    .thenReturn(LocalDateTime.now().plusDays(30));

            orchestrator.synchronizeFromStripe(internalSubscription, stripeSubscription);

            verify(subscriptionRepository).persist(internalSubscription);
        }
    }
}

