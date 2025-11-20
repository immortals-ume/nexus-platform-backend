package com.immortals.authapp.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeUtils {
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
