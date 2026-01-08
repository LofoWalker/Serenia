package com.lofo.serenia.exception.exceptions;

import lombok.Getter;

/**
 * Types de quotas pouvant être dépassés.
 */
@Getter
public enum QuotaType {

    MONTHLY_TOKEN_LIMIT("monthly_token_limit", "Monthly token limit exceeded"),
    DAILY_MESSAGE_LIMIT("daily_message_limit", "Daily message limit reached");

    private final String code;
    private final String defaultMessage;

    QuotaType(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
