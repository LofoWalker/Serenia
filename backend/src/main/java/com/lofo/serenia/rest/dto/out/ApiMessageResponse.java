package com.lofo.serenia.rest.dto.out;

/**
 * Generic response containing a single message.
 * Used for various endpoints like registration, password reset, account activation, etc.
 */
public record ApiMessageResponse(String message) {
}

