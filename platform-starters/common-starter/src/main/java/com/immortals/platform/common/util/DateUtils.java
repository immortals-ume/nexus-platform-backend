package com.immortals.platform.common.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateUtils {

    /* =========================
       Clock (for testability)
       ========================= */
    private static Clock clock = Clock.systemDefaultZone();

    /** Override clock (used only in tests) */
    public static void setClock(Clock newClock) {
        clock = Objects.requireNonNull(newClock);
    }

    /** Reset to system clock */
    public static void resetClock() {
        clock = Clock.systemDefaultZone();
    }

    /* =========================
       Common Formatters
       ========================= */
    public static final DateTimeFormatter ISO_DATE_TIME =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ISO_LOCAL_DATE;

    public static final DateTimeFormatter ISO_TIME =
            DateTimeFormatter.ISO_LOCAL_TIME;

    public static final DateTimeFormatter DATE_TIME_WITH_ZONE =
            DateTimeFormatter.ISO_ZONED_DATE_TIME;

    public static final DateTimeFormatter READABLE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* =========================
       Now helpers
       ========================= */
    public static LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public static LocalDate today() {
        return LocalDate.now(clock);
    }

    public static LocalTime currentTime() {
        return LocalTime.now(clock);
    }

    public static Instant instantNow() {
        return Instant.now(clock);
    }

    public static ZonedDateTime nowInZone(ZoneId zoneId) {
        return ZonedDateTime.now(clock.withZone(zoneId));
    }

    /* =========================
       Formatting
       ========================= */
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(ISO_DATE_TIME);
    }

    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime.format(formatter);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /* =========================
       Parsing
       ========================= */
    public static LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value, ISO_DATE_TIME);
    }

    public static LocalDateTime parseDateTime(String value, String pattern) {
        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDate parseDate(String value) {
        return LocalDate.parse(value, ISO_DATE);
    }

    public static boolean isValidDateTime(String value, DateTimeFormatter formatter) {
        try {
            LocalDateTime.parse(value, formatter);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    /* =========================
       Conversions
       ========================= */

    // LocalDateTime ↔ Instant
    public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
        return dateTime.atZone(zoneId).toInstant();
    }

    public static LocalDateTime fromInstant(Instant instant, ZoneId zoneId) {
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    // Legacy Date ↔ java.time
    public static Date toDate(Instant instant) {
        return Date.from(instant);
    }

    public static Instant toInstant(Date date) {
        return date.toInstant();
    }

    /* =========================
       Arithmetic
       ========================= */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime.plusDays(days);
    }

    public static LocalDateTime minusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime.minusMinutes(minutes);
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /* =========================
       Comparison
       ========================= */
    public static boolean isBeforeNow(LocalDateTime dateTime) {
        return dateTime.isBefore(now());
    }

    public static boolean isAfterNow(LocalDateTime dateTime) {
        return dateTime.isAfter(now());
    }

    public static boolean isBetween(
            LocalDateTime target,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return !target.isBefore(start) && !target.isAfter(end);
    }

    /* =========================
       Duration / Difference
       ========================= */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /* =========================
       Zone helpers
       ========================= */
    public static ZonedDateTime toZonedDateTime(
            LocalDateTime dateTime,
            ZoneId zoneId
    ) {
        return dateTime.atZone(zoneId);
    }

    public static LocalDateTime convertZone(
            LocalDateTime dateTime,
            ZoneId from,
            ZoneId to
    ) {
        return dateTime
                .atZone(from)
                .withZoneSameInstant(to)
                .toLocalDateTime();
    }
}
