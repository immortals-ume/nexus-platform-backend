package com.immortals.platform.domain.helper;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class TokenBucket implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int tokens;
    private long lastRefillTimestamp;

    public TokenBucket(int tokens, long lastRefillTimestamp) {
        this.tokens = tokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }

}
