package com.lofo.serenia.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class AuthenticationFailedException extends WebApplicationException {

    public AuthenticationFailedException(String message) {
        super(message, Response.Status.UNAUTHORIZED);
    }
}

