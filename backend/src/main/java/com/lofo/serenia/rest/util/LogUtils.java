package com.lofo.serenia.rest.util;

/**
 * Utility methods for safe log output.
 */
public final class LogUtils {

    private LogUtils() {}

    /**
     * Masks a token for logging, showing only first 4 and last 4 characters.
     * Prevents sensitive token values from appearing in plain text in logs.
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}

