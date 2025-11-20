package com.immortals.otpservice.manager;

import java.time.Duration;

public interface TokenLockManager {
    void acquireRead(String key, Duration duration) throws InterruptedException;
    void releaseRead(String key);

    void acquireWrite(String key) throws InterruptedException;
    void releaseWrite(String key);
}
