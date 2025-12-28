package com.lofo.serenia.rest.dto.out;

/**
 * DTO containing the Stripe Checkout session URL.
 * The frontend redirects the user to this URL to complete the payment.
 */
public record CheckoutSessionDTO(
        String sessionId,
        String url
) {
}

