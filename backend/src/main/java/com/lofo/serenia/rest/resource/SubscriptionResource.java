package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.in.ChangePlanRequestDTO;
import com.lofo.serenia.rest.dto.out.PlanDTO;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import com.lofo.serenia.service.subscription.SubscriptionService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;
/**
 * Resource REST pour la gestion des subscriptions et l'observabilité des quotas.
 */
@Authenticated
@Path("/api/subscription")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Subscription", description = "Manage subscription and view quota status")
public class SubscriptionResource {
    private final SubscriptionService subscriptionService;
    private final JsonWebToken jwt;
    public SubscriptionResource(SubscriptionService subscriptionService, JsonWebToken jwt) {
        this.subscriptionService = subscriptionService;
        this.jwt = jwt;
    }

    @GET
    @Path("/plans")
    @PermitAll
    @Operation(
            summary = "Get available plans",
            description = "Returns the list of all available subscription plans with their limits."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Plans returned successfully",
                    content = @Content(schema = @Schema(implementation = PlanDTO[].class))
            )
    })
    public Response getPlans() {
        List<PlanDTO> plans = subscriptionService.getAllPlans();
        return Response.ok(plans).build();
    }

    @GET
    @Path("/status")
    @Operation(
            summary = "Get subscription status",
            description = "Returns the current subscription status including plan details, " +
                    "remaining quotas, and reset dates for the authenticated user."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Subscription status returned successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionStatusDTO.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "User not authenticated"
            )
    })
    public Response getStatus() {
        UUID userId = getAuthenticatedUserId();
        SubscriptionStatusDTO status = subscriptionService.getStatus(userId);
        return Response.ok(status).build();
    }

    @PUT
    @Path("/plan")
    @Operation(
            summary = "Change subscription plan",
            description = "Changes the subscription plan for the authenticated user."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Plan changed successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionStatusDTO.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid plan type"
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "User not authenticated"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Plan not found"
            )
    })
    public Response changePlan(@Valid ChangePlanRequestDTO request) {
        UUID userId = getAuthenticatedUserId();
        subscriptionService.changePlan(userId, request.planType());
        SubscriptionStatusDTO status = subscriptionService.getStatus(userId);
        return Response.ok(status).build();
    }

    private UUID getAuthenticatedUserId() {
        String subject = jwt.getSubject();
        return UUID.fromString(subject);
    }
}
