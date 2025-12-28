package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.in.ForgotPasswordRequest;
import com.lofo.serenia.rest.dto.in.ResetPasswordRequest;
import com.lofo.serenia.rest.dto.out.ApiMessageResponse;
import com.lofo.serenia.service.user.password.PasswordResetService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource for password recovery and reset operations.
 * Handles password reset requests and token-based password resets.
 */
@Slf4j
@Path("/api/password")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Password Management")
public class PasswordManagementResource {

    private final PasswordResetService passwordResetService;

    @Inject
    public PasswordManagementResource(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Requests a password reset by sending a reset email to the user.
     * Returns 200 regardless of whether the email exists to prevent user enumeration attacks.
     *
     * @param request the password reset request containing the email address
     * @return 200 OK with success message (regardless of user existence)
     */
    @POST
    @Path("/forgot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Request password reset",
               description = "Sends a password reset email if the user exists. Returns 200 regardless of email existence (prevents user enumeration).")
    @RequestBody(content = @Content(schema = @Schema(implementation = ForgotPasswordRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Password reset request processed",
                        content = @Content(schema = @Schema(implementation = ApiMessageResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload")
    })
    public Response requestPasswordReset(@Valid ForgotPasswordRequest request) {
        log.info("Password reset requested for email=%s", request.email());
        passwordResetService.requestPasswordReset(request.email());
        return Response.ok()
                .entity(new ApiMessageResponse(
                        "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."))
                .build();
    }

    /**
     * Resets a user's password using a valid reset token.
     * The token must be obtained from the password reset email.
     *
     * @param request the reset password request containing token and new password
     * @return 200 OK on successful reset, or 400 Bad Request if token is invalid/expired
     */
    @POST
    @Path("/reset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reset password",
               description = "Resets the user's password using a valid token from the password reset email.")
    @RequestBody(content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Password reset successfully",
                        content = @Content(schema = @Schema(implementation = ApiMessageResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid or expired reset token")
    })
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        log.info("Password reset initiated with token=%s", maskToken(request.token()));
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return Response.ok()
                .entity(new ApiMessageResponse(
                        "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter."))
                .build();
    }

    /**
     * Masks a token for logging purposes (shows only first 4 and last 4 characters).
     *
     * @param token the token to mask
     * @return masked token representation
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}

