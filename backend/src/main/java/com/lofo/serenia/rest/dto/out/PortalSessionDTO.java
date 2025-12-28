package com.lofo.serenia.rest.dto.out;

/**
 * DTO containing the Stripe Customer Portal session URL.
 * The frontend redirects the user to this URL to manage their subscription.
 */
public record PortalSessionDTO(
        String url
) {
}

