package com.immortals.usermanagementservice.model.helper;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TokenBucket implements Serializable {
    private int tokens;
    private long lastRefillTimestamp;

    public TokenBucket(int tokens, long lastRefillTimestamp) {
        this.tokens = tokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }

}
