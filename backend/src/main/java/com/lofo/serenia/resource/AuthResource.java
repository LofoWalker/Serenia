package com.lofo.serenia.resource;

import com.lofo.serenia.dto.in.ForgotPasswordRequest;
import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.in.ResetPasswordRequest;
import com.lofo.serenia.dto.out.ActivationResponseDTO;
import com.lofo.serenia.dto.out.ApiMessageResponse;
import com.lofo.serenia.dto.out.AuthResponseDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.service.auth.EmailVerificationService;
import com.lofo.serenia.service.auth.PasswordService;
import com.lofo.serenia.service.auth.UserAuthenticationService;
import com.lofo.serenia.service.auth.UserRegistrationService;
import com.lofo.serenia.service.token.TokenService;
import com.lofo.serenia.service.user.UserLifecycleService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    private final UserRegistrationService userRegistrationService;
    private final UserAuthenticationService userAuthenticationService;
    private final UserLifecycleService userLifecycleService;
    private final SecurityIdentity securityIdentity;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordService passwordService;

    public AuthResource(UserRegistrationService userRegistrationService,
            UserAuthenticationService userAuthenticationService,
            UserLifecycleService userLifecycleService,
            SecurityIdentity securityIdentity,
            TokenService tokenService,
            EmailVerificationService emailVerificationService,
            PasswordService passwordService) {
        this.userRegistrationService = userRegistrationService;
        this.userAuthenticationService = userAuthenticationService;
        this.userLifecycleService = userLifecycleService;
        this.securityIdentity = securityIdentity;
        this.tokenService = tokenService;
        this.emailVerificationService = emailVerificationService;
        this.passwordService = passwordService;
    }

    @POST
    @Path("/register")
    @Operation(summary = "Register user", description = "Creates a user and sends an activation email.")
    @RequestBody(content = @Content(schema = @Schema(implementation = RegistrationRequestDTO.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "User registered", content = @Content(schema = @Schema(implementation = ApiMessageResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload")
    })
    public Response register(@Valid RegistrationRequestDTO dto) {
        LOG.infof("REST register called for email=%s", dto.email());
        userRegistrationService.register(dto);
        return Response.status(Response.Status.CREATED)
                .entity(new ApiMessageResponse(
                        "Inscription réussie. Un lien d'activation a été envoyé à votre email."))
                .build();
    }

    @GET
    @Path("/activate")
    @Operation(summary = "Activate account", description = "Validates an activation token and unlocks the account.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Account activated", content = @Content(schema = @Schema(implementation = ActivationResponseDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid or missing token")
    })
    public Response activate(
            @QueryParam("token") @Parameter(description = "Activation token", required = true) String token) {
        LOG.infof("REST activate called with token=%s", maskToken(token));
        if (token == null || token.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ActivationResponseDTO("Jeton invalide"))
                    .build();
        }
        emailVerificationService.activateAccount(token);
        return Response
                .ok(new ActivationResponseDTO("Compte activé avec succès. Vous pouvez maintenant vous connecter."))
                .build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Login", description = "Authenticates the user and returns a JWT plus profile info.")
    @RequestBody(content = @Content(schema = @Schema(implementation = LoginRequestDTO.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Authenticated", content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @APIResponse(responseCode = "401", description = "Invalid credentials"),
            @APIResponse(responseCode = "403", description = "Account not activated")
    })
    public Response login(@Valid LoginRequestDTO dto) {
        LOG.infof("REST login called for email=%s", dto.email());
        UserResponseDTO view = userAuthenticationService.login(dto);
        String token = tokenService.generateToken(view);
        LOG.debugf("JWT generated for email=%s", dto.email());
        return Response.ok(new AuthResponseDTO(view, token)).build();
    }

    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "Current user", description = "Returns the authenticated user's profile.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profile returned", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @APIResponse(responseCode = "401", description = "User not authenticated")
    })
    public Response me() {
        String email = securityIdentity.getPrincipal().getName();
        LOG.debugf("REST me called for email=%s", email);
        UserResponseDTO view = userAuthenticationService.getByEmail(email);
        return Response.ok(view).build();
    }

    @DELETE
    @Path("/me")
    @Authenticated
    @Operation(summary = "Delete current user", description = "Deletes the authenticated user's account.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Account deleted"),
            @APIResponse(responseCode = "401", description = "User not authenticated")
    })
    public Response deleteMe() {
        String email = securityIdentity.getPrincipal().getName();
        userLifecycleService.deleteAccountAndAssociatedData(email);
        return Response.noContent().build();
    }

    @POST
    @Path("/forgot-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Request password reset", description = "Sends a password reset email if the user exists. Returns 200 regardless of whether the email exists (prevents user enumeration).")
    @RequestBody(content = @Content(schema = @Schema(implementation = ForgotPasswordRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Request processed"),
            @APIResponse(responseCode = "400", description = "Invalid payload")
    })
    public Response forgotPassword(@Valid ForgotPasswordRequest request) {
        LOG.infof("REST forgot-password called for email=%s", request.email());
        passwordService.requestReset(request.email());
        return Response.ok()
                .entity(new ApiMessageResponse("Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."))
                .build();
    }

    @POST
    @Path("/reset-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reset password", description = "Resets the user's password using a valid token.")
    @RequestBody(content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Password reset successfully"),
            @APIResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        LOG.infof("REST reset-password called with token=%s", maskToken(request.token()));
        passwordService.resetPassword(request.token(), request.newPassword());
        return Response.ok()
                .entity(new ApiMessageResponse("Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter."))
                .build();
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}
