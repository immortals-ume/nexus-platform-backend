package com.immortals.authapp.service;

public interface LoginAttempt {
    void loginSucceeded(String username);

    void loginFailed(String username);

    boolean isBlocked(String username);
}
