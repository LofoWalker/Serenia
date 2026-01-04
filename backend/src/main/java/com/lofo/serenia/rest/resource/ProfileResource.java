package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.user.account.AccountManagementService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource for authenticated user profile management.
 * Requires valid JWT authentication for all operations.
 */
@Slf4j
@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Profile")
public class ProfileResource {

    private final AccountManagementService accountManagementService;
    private final SecurityIdentity securityIdentity;

    @Inject
    public ProfileResource(AccountManagementService accountManagementService, SecurityIdentity securityIdentity) {
        this.accountManagementService = accountManagementService;
        this.securityIdentity = securityIdentity;
    }

    /**
     * Retrieves the current authenticated user's profile information.
     *
     * @return 200 OK with user profile data, or 401 Unauthorized if not authenticated
     */
    @GET
    @Operation(summary = "Get current user profile",
               description = "Returns the authenticated user's profile information.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User profile retrieved successfully",
                        content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @APIResponse(responseCode = "401", description = "User not authenticated")
    })
    public Response getProfile() {
        String email = securityIdentity.getPrincipal().getName();
        log.debug("User profile requested for email=%s", email);

        UserResponseDTO userProfile = accountManagementService.getUserProfile(email);
        return Response.ok(userProfile).build();
    }

    /**
     * Deletes the current authenticated user's account and all associated data.
     * This operation is irreversible.
     *
     * @return 204 No Content on successful deletion, or 401 Unauthorized if not authenticated
     */
    @DELETE
    @Operation(summary = "Delete current user account",
               description = "Permanently deletes the authenticated user's account and all associated data.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Account successfully deleted"),
            @APIResponse(responseCode = "401", description = "User not authenticated")
    })
    public Response deleteProfile() {
        String email = securityIdentity.getPrincipal().getName();
        log.info("User account deletion requested for email=%s", email);

        accountManagementService.deleteAccountAndAssociatedData(email);
        return Response.noContent().build();
    }
}

