package com.lofo.serenia.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class UnactivatedAccountException extends WebApplicationException {
    public static final String USER_MESSAGE = "Veuillez activer votre compte en consultant l'email que nous vous avons envoyé.";

    public UnactivatedAccountException() {
        this(USER_MESSAGE);
    }

    public UnactivatedAccountException(String message) {
        super(message, Response.status(Response.Status.UNAUTHORIZED).entity(message).build());
    }
}
