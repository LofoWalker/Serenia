package com.lofo.serenia.exception.exceptions;

/**
 * Exception thrown when a user tries to access a resource they don't own.
 */
public class ForbiddenAccessException extends RuntimeException {

    public ForbiddenAccessException(String message) {
        super(message);
    }
}

