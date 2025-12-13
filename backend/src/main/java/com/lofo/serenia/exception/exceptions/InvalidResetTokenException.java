package com.lofo.serenia.exception.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class InvalidResetTokenException extends WebApplicationException {

    public InvalidResetTokenException(String message) {
        super(message, Response.Status.BAD_REQUEST);
    }
}

