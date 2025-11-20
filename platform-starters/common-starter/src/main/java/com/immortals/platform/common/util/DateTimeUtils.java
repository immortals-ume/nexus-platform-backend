package com.immortals.platform.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date and time operations.
 * Provides common date/time manipulation and formatting functions.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Common date/time formatters
    public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    public static final DateTimeFormatter ISO_TIME_FORMATTER = DateTimeFormatter.ISO_TIME;

    /**
     * Gets the current UTC instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Gets the current date in UTC
     */
    public static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Gets the current date-time in UTC
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Converts an Instant to LocalDateTime in UTC
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * Converts a LocalDateTime to Instant (assumes UTC)
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Converts a LocalDate to Instant at start of day (UTC)
     */
    public static Instant toInstant(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Formats an Instant as ISO 8601 string
     */
    public static String formatIso(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_DATE_TIME_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
    }

    /**
     * Formats a LocalDateTime as ISO 8601 string
     */
    public static String formatIso(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return ISO_DATE_TIME_FORMATTER.format(localDateTime);
    }

    /**
     * Formats a LocalDate as ISO 8601 string
     */
    public static String formatIso(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return ISO_DATE_FORMATTER.format(localDate);
    }

    /**
     * Parses an ISO 8601 string to Instant
     */
    public static Instant parseInstant(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        return Instant.parse(isoString);
    }

    /**
     * Parses an ISO 8601 string to LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(isoString, ISO_DATE_TIME_FORMATTER);
    }

    /**
     * Parses an ISO 8601 string to LocalDate
     */
    public static LocalDate parseLocalDate(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        return LocalDate.parse(isoString, ISO_DATE_FORMATTER);
    }

    /**
     * Adds days to an Instant
     */
    public static Instant plusDays(Instant instant, long days) {
        if (instant == null) {
            return null;
        }
        return instant.plus(days, ChronoUnit.DAYS);
    }

    /**
     * Adds hours to an Instant
     */
    public static Instant plusHours(Instant instant, long hours) {
        if (instant == null) {
            return null;
        }
        return instant.plus(hours, ChronoUnit.HOURS);
    }

    /**
     * Adds minutes to an Instant
     */
    public static Instant plusMinutes(Instant instant, long minutes) {
        if (instant == null) {
            return null;
        }
        return instant.plus(minutes, ChronoUnit.MINUTES);
    }

    /**
     * Adds seconds to an Instant
     */
    public static Instant plusSeconds(Instant instant, long seconds) {
        if (instant == null) {
            return null;
        }
        return instant.plusSeconds(seconds);
    }

    /**
     * Checks if an Instant is in the past
     */
    public static boolean isPast(Instant instant) {
        if (instant == null) {
            return false;
        }
        return instant.isBefore(Instant.now());
    }

    /**
     * Checks if an Instant is in the future
     */
    public static boolean isFuture(Instant instant) {
        if (instant == null) {
            return false;
        }
        return instant.isAfter(Instant.now());
    }

    /**
     * Calculates the duration between two Instants in seconds
     */
    public static long durationInSeconds(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).getSeconds();
    }

    /**
     * Calculates the duration between two Instants in minutes
     */
    public static long durationInMinutes(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toMinutes();
    }

    /**
     * Calculates the duration between two Instants in hours
     */
    public static long durationInHours(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toHours();
    }

    /**
     * Gets the start of day for a given Instant in UTC
     */
    public static Instant startOfDay(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    /**
     * Gets the end of day for a given Instant in UTC
     */
    public static Instant endOfDay(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atTime(LocalTime.MAX)
                .atZone(ZoneOffset.UTC)
                .toInstant();
    }
}
