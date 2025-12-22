package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.in.LoginRequestDTO;
import com.lofo.serenia.rest.dto.out.AuthResponseDTO;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.user.AuthenticationService;
import com.lofo.serenia.service.user.TokenService;
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
 * REST resource for user authentication and login.
 * Handles credentials validation and JWT token generation.
 */
@Slf4j
@Path("/api/auth/login")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication")
public class AuthenticationResource {

    private final AuthenticationService authenticationService;
    private final TokenService tokenService;

    @Inject
    public AuthenticationResource(AuthenticationService authenticationService, TokenService tokenService) {
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    /**
     * Authenticates a user with email and password credentials.
     * Returns a JWT token and user profile information upon successful login.
     *
     * @param dto the login credentials (email and password)
     * @return 200 OK with JWT token and user profile, or 401/403 on failure
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Login",
               description = "Authenticates the user with credentials and returns a JWT token plus profile information.")
    @RequestBody(content = @Content(schema = @Schema(implementation = LoginRequestDTO.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Authentication successful",
                        content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @APIResponse(responseCode = "401", description = "Invalid credentials"),
            @APIResponse(responseCode = "403", description = "Account not activated or locked")
    })
    public Response login(@Valid LoginRequestDTO dto) {
        log.info("User login attempted for email=%s", dto.email());

        UserResponseDTO userProfile = authenticationService.login(dto);
        String token = tokenService.generateToken(userProfile);

        log.debug("JWT token successfully generated for email=%s", dto.email());
        return Response.ok(new AuthResponseDTO(userProfile, token)).build();
    }
}

