package com.lofo.serenia.exception;

import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.exception.exceptions.InvalidTokenException;
import com.lofo.serenia.rest.dto.out.ApiMessageResponse;
import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.exceptions.UnactivatedAccountException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Global exception mapper for authentication and user-related exceptions.
 *
 * Maps business exceptions to appropriate HTTP responses with meaningful error messages.
 */
@Provider
public class AuthExceptionMapper implements ExceptionMapper<RuntimeException> {

    private static final Logger LOG = Logger.getLogger(AuthExceptionMapper.class);
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    @Override
    public Response toResponse(RuntimeException exception) {
        LOG.debugf("AuthExceptionMapper: handling exception=%s, message=%s",
                   exception.getClass().getSimpleName(), exception.getMessage());

        if (exception instanceof AuthenticationFailedException ex) {
            return handleAuthenticationFailed(ex);
        }
        if (exception instanceof UnactivatedAccountException ex) {
            return handleUnactivatedAccount(ex);
        }
        if (exception instanceof InvalidTokenException ex) {
            return handleInvalidResetToken(ex);
        }
        if (exception instanceof ForbiddenAccessException ex) {
            return handleForbiddenAccess(ex);
        }
        if (exception instanceof NotFoundException ex) {
            return handleNotFoundException(ex);
        }

        LOG.warnf("Unhandled exception: %s - %s", exception.getClass().getName(), exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ApiMessageResponse("Une erreur interne est survenue"))
                .build();
    }

    /**
     * Handles authentication failures (invalid credentials, user not found).
     *
     * Returns 401 Unauthorized with generic message to prevent user enumeration.
     */
    private Response handleAuthenticationFailed(AuthenticationFailedException ex) {
        LOG.debugf("AuthExceptionMapper: AuthenticationFailedException - %s", ex.getMessage());
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ApiMessageResponse("Email ou mot de passe invalide"))
                .build();
    }

    /**
     * Handles unactivated account attempts.
     *
     * Returns 403 Forbidden with message asking user to activate.
     */
    private Response handleUnactivatedAccount(UnactivatedAccountException ex) {
        LOG.debug("AuthExceptionMapper: UnactivatedAccountException");
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ApiMessageResponse(ex.getMessage()))
                .build();
    }

    /**
     * Handles invalid or expired password reset tokens.
     *
     * Returns 400 Bad Request with appropriate message.
     */
    private Response handleInvalidResetToken(InvalidTokenException ex) {
        LOG.debugf("AuthExceptionMapper: InvalidResetTokenException - %s", ex.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiMessageResponse(ex.getMessage()))
                .build();
    }

    /**
     * Handles NotFoundException cases.
     *
     * Returns 401 Unauthorized if user not found (token references deleted user),
     * otherwise returns 404 Not Found for other resources.
     */
    private Response handleNotFoundException(NotFoundException ex) {
        LOG.debugf("AuthExceptionMapper: NotFoundException - %s", ex.getMessage());

        // If user not found, return 401 (token is valid but user no longer exists)
        if (USER_NOT_FOUND_MESSAGE.equals(ex.getMessage())) {
            LOG.debug("User not found - returning 401 Unauthorized");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiMessageResponse("Utilisateur non authentifié"))
                    .build();
        }

        // For other resources, return 404
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ApiMessageResponse(ex.getMessage()))
                .build();
    }

    /**
     * Handles forbidden access attempts.
     *
     * Returns 403 Forbidden when user tries to access a resource they don't own.
     */
    private Response handleForbiddenAccess(ForbiddenAccessException ex) {
        LOG.debugf("AuthExceptionMapper: ForbiddenAccessException - %s", ex.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ApiMessageResponse(ex.getMessage()))
                .build();
    }
}

