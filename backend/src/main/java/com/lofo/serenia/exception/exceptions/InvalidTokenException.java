package com.lofo.serenia.exception.exceptions;

import io.quarkus.security.UnauthorizedException;

public class InvalidTokenException extends UnauthorizedException {

    public InvalidTokenException(String message) {
        super(message);
    }
}

