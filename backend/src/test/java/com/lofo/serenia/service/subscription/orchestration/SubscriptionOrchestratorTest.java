package com.lofo.serenia.service.subscription.orchestration;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.mapper.DateTimeConverter;
import com.lofo.serenia.service.subscription.mapper.StripeStatusMapper;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private PanacheQuery<Plan> planQuery;

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

        // In stripe-java 29.x, currentPeriodEnd lives on SubscriptionItem, not on Subscription
        Price price = new Price();
        price.setId("price_test123");

        SubscriptionItem item = new SubscriptionItem();
        item.setCurrentPeriodEnd(System.currentTimeMillis() / 1000);
        item.setPrice(price);

        SubscriptionItemCollection items = new SubscriptionItemCollection();
        items.setData(List.of(item));
        stripeSubscription.setItems(items);
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
            when(planRepository.find("stripePriceId", "price_test123")).thenReturn(planQuery);
            when(planQuery.firstResultOptional()).thenReturn(Optional.of(planPlus));

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
            // In stripe-java 29.x, discounts is a list - single setDiscount() is replaced by setDiscountObjects()
            stripeSubscription.setDiscountObjects(List.of(stripeDiscount));

            when(statusMapper.mapStatus("active")).thenReturn(SubscriptionStatus.ACTIVE);
            when(dateTimeConverter.convertEpochToDateTime(anyLong()))
                    .thenReturn(LocalDateTime.now().plusDays(30));
            when(planRepository.find("stripePriceId", "price_test123")).thenReturn(planQuery);
            when(planQuery.firstResultOptional()).thenReturn(Optional.of(planPlus));

            orchestrator.synchronizeFromStripe(internalSubscription, stripeSubscription);

            verify(subscriptionRepository).persist(internalSubscription);
        }
    }
}

