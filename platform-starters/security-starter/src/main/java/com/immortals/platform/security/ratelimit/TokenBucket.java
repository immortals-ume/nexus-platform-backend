package com.immortals.platform.security.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token bucket for rate limiting.
 * Implements the token bucket algorithm for distributed rate limiting.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBucket implements Serializable {
    private int tokens;
    private long lastRefillTimestamp;
}
