package com.lofo.serenia.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ForbiddenAccessException extends WebApplicationException {

    public ForbiddenAccessException(String message) {
        super(message, Response.Status.FORBIDDEN);
    }
}

