package com.lofo.serenia.service.subscription;
import com.lofo.serenia.persistence.entity.subscription.DiscountType;
import com.stripe.model.Discount;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
/**
 * Helper class to extract and process discount information from Stripe objects.
 * Stripe is the source of truth for discount data.
 * Supports discounts from both subscriptions (Discount object) and checkout sessions (discounts list).
 */
@Slf4j
public class StripeDiscountHelper {
    /**
     * Extracts discount data from a Stripe Discount object (from subscription).
     * Returns a DiscountData object containing coupon ID, type, value, and end date.
     *
     * @param discount the Stripe Discount object (may be null)
     * @return DiscountData with populated fields, or null if no discount
     */
    public static DiscountData extractDiscountData(Discount discount) {
        if (discount == null || discount.getCoupon() == null) {
            return null;
        }
        try {
            com.stripe.model.Coupon coupon = discount.getCoupon();
            String couponId = coupon.getId();
            if (couponId == null) {
                log.warn("Discount found but no coupon ID available");
                return null;
            }
            DiscountType discountType = extractDiscountType(coupon);
            Double discountValue = extractDiscountValue(coupon);
            LocalDateTime discountEndDate = extractDiscountEndDate(discount);
            log.debug("Extracted discount from subscription: couponId={}, type={}, value={}, endDate={}",
                    couponId, discountType, discountValue, discountEndDate);
            return new DiscountData(couponId, discountType, discountValue, discountEndDate);
        } catch (Exception e) {
            log.error("Failed to extract discount data from subscription: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Determines the discount type from a Stripe Coupon object.
     * Maps Stripe coupon percentOff and amountOff to our DiscountType enum.
     */
    private static DiscountType extractDiscountType(com.stripe.model.Coupon coupon) {
        if (coupon.getPercentOff() != null) {
            return DiscountType.PERCENTAGE;
        }
        if (coupon.getAmountOff() != null) {
            return DiscountType.AMOUNT;
        }
        // Default to PERCENTAGE if both are absent (shouldn't happen in practice)
        return DiscountType.PERCENTAGE;
    }
    /**
     * Extracts the discount value from a Stripe Coupon object.
     * Returns percentage (e.g., 10.0) or amount in the coupon's currency.
     */
    private static Double extractDiscountValue(com.stripe.model.Coupon coupon) {
        if (coupon.getPercentOff() != null) {
            return coupon.getPercentOff().doubleValue();
        }
        if (coupon.getAmountOff() != null) {
            // Convert cents to currency units (e.g., 500 cents = 5.00)
            return coupon.getAmountOff().doubleValue() / 100.0;
        }
        return null;
    }
    /**
     * Extracts the discount end date from a Stripe Discount object.
     * Accounts for recurring vs. one-time discounts.
     * Returns null for permanent discounts.
     */
    private static LocalDateTime extractDiscountEndDate(Discount discount) {
        // If durationInMonths is set, the discount is valid for that many months
        if (discount.getCoupon().getDurationInMonths() != null) {
            Long durationMonths = discount.getCoupon().getDurationInMonths();
            Long startEpoch = discount.getStart();
            if (startEpoch != null) {
                LocalDateTime startDate = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(startEpoch),
                        ZoneId.systemDefault()
                );
                return startDate.plusMonths(durationMonths);
            }
        }
        // If endDate is set, use it directly
        if (discount.getEnd() != null) {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(discount.getEnd()),
                    ZoneId.systemDefault()
            );
        }
        // No end date = permanent discount
        return null;
    }
    /**
     * Checks if a discount has expired based on the stored end date.
     * Returns false if no end date exists (permanent discount).
     */
    public static boolean isDiscountExpired(LocalDateTime discountEndDate) {
        if (discountEndDate == null) {
            return false; // Permanent discount
        }
        return LocalDateTime.now().isAfter(discountEndDate);
    }
    /**
     * Record class to hold extracted discount data.
     */
    public record DiscountData(
            String couponId,
            DiscountType type,
            Double value,
            LocalDateTime endDate
    ) {
    }
}
