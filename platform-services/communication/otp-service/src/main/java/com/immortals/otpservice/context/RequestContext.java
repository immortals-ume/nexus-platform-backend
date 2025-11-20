package com.immortals.otpservice.context;

public class RequestContext {

    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();
    public static String getUserAgent() {
        return USER_AGENT.get();
    }
    public static void setUserAgent(String userAgent) {
        USER_AGENT.set(userAgent);
    }

    public static void clear() {
        USER_AGENT.remove();
    }
}
