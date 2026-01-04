package com.lofo.serenia.service.subscription.discount;

import com.lofo.serenia.persistence.entity.subscription.DiscountType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.service.subscription.StripeDiscountHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountProcessor Tests")
class DiscountProcessorTest {

    private DiscountProcessor discountProcessor;

    @Mock
    private Subscription subscription;

    @Mock
    private com.stripe.model.Discount stripeDiscount;

    @BeforeEach
    void setUp() {
        discountProcessor = new DiscountProcessor();
    }

    @Nested
    @DisplayName("applyDiscount")
    class ApplyDiscount {

        @Test
        @DisplayName("should apply discount data when discount data is extracted")
        void should_apply_discount_data() {
            StripeDiscountHelper.DiscountData discountData = new StripeDiscountHelper.DiscountData(
                    "coupon_test123",
                    DiscountType.PERCENTAGE,
                    10.0,
                    LocalDateTime.now().plusMonths(3)
            );

            try (MockedStatic<StripeDiscountHelper> mockedHelper = mockStatic(StripeDiscountHelper.class)) {
                mockedHelper.when(() -> StripeDiscountHelper.extractDiscountData(stripeDiscount))
                        .thenReturn(discountData);

                discountProcessor.applyDiscount(subscription, stripeDiscount);

                verify(subscription).setStripeCouponId("coupon_test123");
                verify(subscription).setDiscountType(DiscountType.PERCENTAGE);
                verify(subscription).setDiscountValue(10.0);
                verify(subscription).setDiscountEndDate(discountData.endDate());
            }
        }

        @Test
        @DisplayName("should clear discount when discount data extraction returns null")
        void should_clear_discount_on_null_data() {
            try (MockedStatic<StripeDiscountHelper> mockedHelper = mockStatic(StripeDiscountHelper.class)) {
                mockedHelper.when(() -> StripeDiscountHelper.extractDiscountData(stripeDiscount))
                        .thenReturn(null);

                discountProcessor.applyDiscount(subscription, stripeDiscount);

                verify(subscription).setStripeCouponId(null);
                verify(subscription).setDiscountType(null);
                verify(subscription).setDiscountValue(null);
                verify(subscription).setDiscountEndDate(null);
            }
        }

        @Test
        @DisplayName("should apply AMOUNT discount type")
        void should_apply_amount_discount() {
            StripeDiscountHelper.DiscountData discountData = new StripeDiscountHelper.DiscountData(
                    "coupon_5eur",
                    DiscountType.AMOUNT,
                    5.0,
                    LocalDateTime.now().plusMonths(1)
            );

            try (MockedStatic<StripeDiscountHelper> mockedHelper = mockStatic(StripeDiscountHelper.class)) {
                mockedHelper.when(() -> StripeDiscountHelper.extractDiscountData(stripeDiscount))
                        .thenReturn(discountData);

                discountProcessor.applyDiscount(subscription, stripeDiscount);

                verify(subscription).setDiscountType(DiscountType.AMOUNT);
                verify(subscription).setDiscountValue(5.0);
            }
        }
    }

    @Nested
    @DisplayName("clearDiscount")
    class ClearDiscount {

        @Test
        @DisplayName("should clear all discount fields")
        void should_clear_all_discount_fields() {
            discountProcessor.clearDiscount(subscription);

            verify(subscription).setStripeCouponId(null);
            verify(subscription).setDiscountType(null);
            verify(subscription).setDiscountValue(null);
            verify(subscription).setDiscountEndDate(null);
        }
    }

    @Nested
    @DisplayName("isDiscountExpired")
    class IsDiscountExpired {

        @Test
        @DisplayName("should return true when discount end date is in the past")
        void should_return_true_when_expired() {
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            when(subscription.getDiscountEndDate()).thenReturn(pastDate);

            try (MockedStatic<StripeDiscountHelper> mockedHelper = mockStatic(StripeDiscountHelper.class)) {
                mockedHelper.when(() -> StripeDiscountHelper.isDiscountExpired(pastDate))
                        .thenReturn(true);

                boolean result = discountProcessor.isDiscountExpired(subscription);

                assertTrue(result);
            }
        }

        @Test
        @DisplayName("should return false when discount end date is in the future")
        void should_return_false_when_not_expired() {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
            when(subscription.getDiscountEndDate()).thenReturn(futureDate);

            try (MockedStatic<StripeDiscountHelper> mockedHelper = mockStatic(StripeDiscountHelper.class)) {
                mockedHelper.when(() -> StripeDiscountHelper.isDiscountExpired(futureDate))
                        .thenReturn(false);

                boolean result = discountProcessor.isDiscountExpired(subscription);

                assertFalse(result);
            }
        }

        @Test
        @DisplayName("should return false when discount end date is null")
        void should_return_false_when_end_date_null() {
            when(subscription.getDiscountEndDate()).thenReturn(null);

            boolean result = discountProcessor.isDiscountExpired(subscription);

            assertFalse(result);
        }
    }
}

