package com.lofo.serenia.rest.resource;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import com.lofo.serenia.service.subscription.SubscriptionService;
import io.quarkus.security.Authenticated;
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
    private UUID getAuthenticatedUserId() {
        String subject = jwt.getSubject();
        return UUID.fromString(subject);
    }
}
