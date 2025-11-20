package com.immortals.otpservice.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeUtils {

    public static Instant calculateExpiry(Long amount, String unit) {
        return Instant.now()
                .plus(amount, ChronoUnit.valueOf(unit.toUpperCase()));
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static Instant nowInstant() {
        return Instant.now();
    }

    public static Duration durationBetween(Instant start, Instant end) {
        return Duration.between(start, end);
    }

}
