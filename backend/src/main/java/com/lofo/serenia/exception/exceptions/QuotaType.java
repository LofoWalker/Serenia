package com.lofo.serenia.exception.exceptions;
/**
 * Types de quotas pouvant être dépassés.
 */
public enum QuotaType {
    MESSAGE_TOKEN_LIMIT("message_token_limit", "Message exceeds token limit"),
    MONTHLY_TOKEN_LIMIT("monthly_token_limit", "Monthly token limit exceeded"),
    DAILY_MESSAGE_LIMIT("daily_message_limit", "Daily message limit reached");
    private final String code;
    private final String defaultMessage;
    QuotaType(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    public String getCode() {
        return code;
    }
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
