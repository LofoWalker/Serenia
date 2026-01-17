package com.lofo.serenia.rest.resource;

import com.lofo.serenia.config.StripeConfig;
import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.service.subscription.webhook.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource for receiving Stripe webhooks.
 * This endpoint must be publicly accessible as Stripe calls it directly.
 */
@Slf4j
@Path("/stripe")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Tag(name = "Stripe Webhook", description = "Stripe webhook endpoint")
public class StripeWebhookResource {

    private static final String ERROR_WEBHOOK_SECRET_NOT_CONFIGURED =
            "Webhook secret not configured";
    private static final String ERROR_INVALID_SIGNATURE =
            "Invalid signature";
    private static final String JSON_RECEIVED_TRUE =
            "{\"received\": true}";
    private static final String JSON_RECEIVED_SKIPPED =
            "{\"received\": true, \"skipped\": \"%s\"}";
    private static final String JSON_ERROR =
            "{\"error\": \"%s\"}";

    private final StripeConfig stripeConfig;
    private final StripeWebhookService webhookService;

    @POST
    @Path("/webhook")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Stripe webhook endpoint",
            description = "Receives webhook events from Stripe. This endpoint must be publicly accessible."
    )
    @APIResponse(
            responseCode = "200",
            description = "Webhook processed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid payload or signature",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal error - Stripe should retry",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Response handleWebhook(
            String payload,
            @HeaderParam("Stripe-Signature") String sigHeader
    ) {
        log.debug("Received Stripe webhook");

        Response secretValidationResponse = validateWebhookSecret();
        if (secretValidationResponse != null) {
            return secretValidationResponse;
        }

        Event event = constructAndVerifyEvent(payload, sigHeader);
        if (event == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format(JSON_ERROR, ERROR_INVALID_SIGNATURE))
                    .build();
        }

        log.info("Processing Stripe event: {} (id: {})", event.getType(), event.getId());

        return processEvent(event);
    }

    /**
     * Validates that the webhook secret is properly configured.
     *
     * @return Response with error if secret is not configured, null if valid
     */
    private Response validateWebhookSecret() {
        String webhookSecret = stripeConfig.webhookSecret();

        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.error("Webhook secret not configured - rejecting request for security");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(String.format(JSON_ERROR, ERROR_WEBHOOK_SECRET_NOT_CONFIGURED))
                    .build();
        }

        return null;
    }

    /**
     * Constructs and verifies the Stripe event using the webhook signature.
     *
     * @param payload the raw webhook payload
     * @param sigHeader the Stripe signature header value
     * @return Event object if signature is valid, null otherwise
     */
    private Event constructAndVerifyEvent(String payload, String sigHeader) {
        if (sigHeader == null || sigHeader.isEmpty()) {
            log.warn("Missing Stripe-Signature header");
            return null;
        }
        try {
            String webhookSecret = stripeConfig.webhookSecret();
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Processes the webhook event and returns appropriate response.
     *
     * @param event the Stripe event to process
     * @return Response indicating success, business error, or system error
     */
    private Response processEvent(Event event) {
        try {
            webhookService.handleEvent(event);
            return Response.ok(JSON_RECEIVED_TRUE).build();
        } catch (WebhookProcessingException e) {
            log.warn("Business error processing webhook event {}: {}", event.getType(), e.getMessage());
            return Response.ok(String.format(JSON_RECEIVED_SKIPPED, e.getMessage())).build();
        } catch (Exception e) {
            log.error("Unexpected error processing webhook event {}: {}", event.getType(), e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(String.format(JSON_ERROR, e.getMessage()))
                    .build();
        }
    }
}

