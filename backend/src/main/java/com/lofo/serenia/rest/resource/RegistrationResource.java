package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.rest.dto.out.ActivationResponseDTO;
import com.lofo.serenia.rest.dto.out.ApiMessageResponse;
import com.lofo.serenia.service.user.activation.AccountActivationService;
import com.lofo.serenia.service.user.registration.RegistrationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource for user registration and account activation.
 * Handles user registration workflow and email verification tokens.
 */
@Slf4j
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Registration")
public class RegistrationResource {

    private final RegistrationService registrationService;
    private final AccountActivationService accountActivationService;

    @Inject
    public RegistrationResource(RegistrationService registrationService, AccountActivationService accountActivationService) {
        this.registrationService = registrationService;
        this.accountActivationService = accountActivationService;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Register user",
               description = "Creates a new user account and sends an activation email.")
    @RequestBody(content = @Content(schema = @Schema(implementation = RegistrationRequestDTO.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "User registered successfully",
                        content = @Content(schema = @Schema(implementation = ApiMessageResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload or user already exists")
    })
    public Response register(@Valid RegistrationRequestDTO dto) {
        log.info("User registration requested for email=%s", dto.email());
        registrationService.register(dto);
        return Response.status(Response.Status.CREATED)
                .entity(new ApiMessageResponse(
                        "Inscription réussie. Un lien d'activation a été envoyé à votre email."))
                .build();
    }

    /**
     * Activates a user account using the activation token from email.
     *
     * @param token the activation token sent via email
     * @return 200 OK with success message or 400 Bad Request if token is invalid
     */
    @GET
    @Path("/activate")
    @Operation(summary = "Activate account",
               description = "Validates an activation token and unlocks the user account.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Account activated successfully",
                        content = @Content(schema = @Schema(implementation = ActivationResponseDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid or missing activation token")
    })
    public Response activate(@QueryParam("token")
                            @Parameter(description = "Activation token from email", required = true)
                            String token) {
        log.info("Account activation requested with token=%s", maskToken(token));

        if (token == null || token.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ActivationResponseDTO("Jeton invalide"))
                    .build();
        }

        accountActivationService.activateAccount(token);
        return Response.ok(new ActivationResponseDTO(
                "Compte activé avec succès. Vous pouvez maintenant vous connecter."))
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

