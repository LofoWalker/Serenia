package com.lofo.serenia.service.subscription.discount;

import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.service.subscription.StripeDiscountHelper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Processes discount operations on subscriptions.
 * Handles application, expiration detection, and clearing of discount data.
 * Delegates discount data extraction to StripeDiscountHelper.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DiscountProcessor {

    /**
     * Applies discount information from a Stripe discount object to a subscription.
     * Extracts coupon ID, discount type, value, and end date.
     *
     * @param subscription the subscription to update
     * @param stripeDiscount the Stripe discount object
     */
    public void applyDiscount(Subscription subscription, com.stripe.model.Discount stripeDiscount) {
        StripeDiscountHelper.DiscountData discountData =
                StripeDiscountHelper.extractDiscountData(stripeDiscount);

        if (discountData != null) {
            subscription.setStripeCouponId(discountData.couponId());
            subscription.setDiscountType(discountData.type());
            subscription.setDiscountValue(discountData.value());
            subscription.setDiscountEndDate(discountData.endDate());
            log.info("Applied discount to subscription: coupon={}, type={}, value={}, endDate={}",
                    discountData.couponId(), discountData.type(), discountData.value(), discountData.endDate());
        } else {
            log.debug("Discount data extraction returned null, clearing discount fields");
            clearDiscount(subscription);
        }
    }

    /**
     * Clears all discount-related fields from a subscription.
     * Used when discount expires or is removed.
     *
     * @param subscription the subscription to clear discount from
     */
    public void clearDiscount(Subscription subscription) {
        subscription.setStripeCouponId(null);
        subscription.setDiscountType(null);
        subscription.setDiscountValue(null);
        subscription.setDiscountEndDate(null);
    }

    /**
     * Checks if a subscription's discount has expired based on its end date.
     *
     * @param subscription the subscription to check
     * @return true if discount end date exists and is in the past, false otherwise
     */
    public boolean isDiscountExpired(Subscription subscription) {
        return subscription.getDiscountEndDate() != null &&
               StripeDiscountHelper.isDiscountExpired(subscription.getDiscountEndDate());
    }
}

