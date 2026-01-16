package com.lofo.serenia.exception.exceptions;
import lombok.Getter;
/**
 * Exception thrown when a usage quota is exceeded.
 * Returns HTTP 429 (Too Many Requests) status.
 */
@Getter
public class QuotaExceededException extends SereniaException {
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private final QuotaType quotaType;
    private final int limit;
    private final int current;
    private final int requested;
    public QuotaExceededException(QuotaType quotaType, int limit, int current, int requested) {
        super(
                buildMessage(quotaType, limit, current, requested),
                HTTP_TOO_MANY_REQUESTS,
                "QUOTA_EXCEEDED_" + quotaType.getCode().toUpperCase()
        );
        this.quotaType = quotaType;
        this.limit = limit;
        this.current = current;
        this.requested = requested;
    }
    private static String buildMessage(QuotaType type, int limit, int current, int requested) {
        return switch (type) {
            case MONTHLY_TOKEN_LIMIT ->
                String.format("Monthly token limit exceeded: %d/%d tokens used, %d requested", 
                        current, limit, requested);
            case DAILY_MESSAGE_LIMIT -> 
                String.format("Daily message limit reached: %d/%d messages sent today", 
                        current, limit);
        };
    }
    public static QuotaExceededException monthlyTokenLimit(int limit, int current, int requested) {
        return new QuotaExceededException(QuotaType.MONTHLY_TOKEN_LIMIT, limit, current, requested);
    }
    public static QuotaExceededException dailyMessageLimit(int limit, int current) {
        return new QuotaExceededException(QuotaType.DAILY_MESSAGE_LIMIT, limit, current, 1);
    }
}
