package com.immortals.platform.common.exception;

import java.io.Serial;

/**
 * Exception for rate limit exceeded errors
 */
public class RateLimitException extends NotificationException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String userId;
    private final String channel;
    private final long retryAfterSeconds;
    
    public RateLimitException(String message, String userId, String channel, long retryAfterSeconds) {
        super(message);
        this.userId = userId;
        this.channel = channel;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
