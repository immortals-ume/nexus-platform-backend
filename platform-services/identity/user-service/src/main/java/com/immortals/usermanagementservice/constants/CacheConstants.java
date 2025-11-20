package com.immortals.usermanagementservice.constants;

public class CacheConstants {
    public static final String USER_HASH_KEY = "users";
    public static final String RATE_LIMITING_HASH_KEY = "rateLimiterBuckets";
    public static final String REFRESH_TOKEN_HASH_KEY = "refresh_Tokens";

    public static final String BLACKLIST_HASH_KEY = "token_blacklisted";
    public static final String ATTEMPT_KEY_PREFIX = "LOGIN_ATTEMPT:";
    public static final String BLOCK_KEY_PREFIX = "LOGIN_BLOCK:";

}
