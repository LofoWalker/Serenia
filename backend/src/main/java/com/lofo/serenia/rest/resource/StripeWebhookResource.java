package com.lofo.serenia.rest.resource;

import com.lofo.serenia.config.StripeConfig;
import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.service.subscription.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
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
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Webhook processed successfully"),
            @APIResponse(responseCode = "400", description = "Invalid payload or signature"),
            @APIResponse(responseCode = "500", description = "Internal error - Stripe should retry")
    })
    public Response handleWebhook(
            String payload,
            @HeaderParam("Stripe-Signature") String sigHeader
    ) {
        log.debug("Received Stripe webhook");

        String webhookSecret = stripeConfig.webhookSecret();

        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.error("Webhook secret not configured - rejecting request for security");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Webhook secret not configured\"}")
                    .build();
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid signature\"}")
                    .build();
        }

        log.info("Processing Stripe event: {} (id: {})", event.getType(), event.getId());

        try {
            webhookService.handleEvent(event);
            return Response.ok("{\"received\": true}").build();
        } catch (WebhookProcessingException e) {
            // Expected business error - return 200 to prevent Stripe retries
            log.warn("Business error processing webhook event {}: {}", event.getType(), e.getMessage());
            return Response.ok("{\"received\": true, \"skipped\": \"" + e.getMessage() + "\"}").build();
        } catch (Exception e) {
            // Unexpected error - return 500 to allow Stripe to retry
            log.error("Unexpected error processing webhook event {}: {}", event.getType(), e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}

