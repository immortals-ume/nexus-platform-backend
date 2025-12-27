package com.immortals.platform.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for StringUtils utility methods.
 */
class StringUtilsTest {

    @Test
    void shouldCheckIfStringIsEmpty() {
        assertThat(StringUtils.isEmpty("")).isTrue();
        assertThat(StringUtils.isEmpty(null)).isTrue();
        assertThat(StringUtils.isEmpty("test")).isFalse();
        assertThat(StringUtils.isEmpty(" ")).isFalse();
    }

    @Test
    void shouldCheckIfStringIsBlank() {
        assertThat(StringUtils.isBlank("")).isTrue();
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank(" ")).isTrue();
        assertThat(StringUtils.isBlank("\t")).isTrue();
        assertThat(StringUtils.isBlank("\n")).isTrue();
        assertThat(StringUtils.isBlank("test")).isFalse();
        assertThat(StringUtils.isBlank(" test ")).isFalse();
    }

    @Test
    void shouldCheckIfStringIsNotEmpty() {
        assertThat(StringUtils.isNotEmpty("")).isFalse();
        assertThat(StringUtils.isNotEmpty(null)).isFalse();
        assertThat(StringUtils.isNotEmpty("test")).isTrue();
        assertThat(StringUtils.isNotEmpty(" ")).isTrue();
    }

    @Test
    void shouldCheckIfStringIsNotBlank() {
        assertThat(StringUtils.isNotBlank("")).isFalse();
        assertThat(StringUtils.isNotBlank(null)).isFalse();
        assertThat(StringUtils.isNotBlank(" ")).isFalse();
        assertThat(StringUtils.isNotBlank("\t")).isFalse();
        assertThat(StringUtils.isNotBlank("test")).isTrue();
        assertThat(StringUtils.isNotBlank(" test ")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "\r"})
    void shouldReturnDefaultValueForBlankStrings(String input) {
        String defaultValue = "default";
        assertThat(StringUtils.defaultIfBlank(input, defaultValue)).isEqualTo(defaultValue);
    }

    @Test
    void shouldReturnOriginalValueForNonBlankStrings() {
        String input = "test";
        String defaultValue = "default";
        assertThat(StringUtils.defaultIfBlank(input, defaultValue)).isEqualTo(input);
    }

    @Test
    void shouldTrimString() {
        assertThat(StringUtils.trim("  test  ")).isEqualTo("test");
        assertThat(StringUtils.trim("test")).isEqualTo("test");
        assertThat(StringUtils.trim("")).isEqualTo("");
        assertThat(StringUtils.trim(null)).isNull();
    }

    @Test
    void shouldTrimToEmpty() {
        assertThat(StringUtils.trimToEmpty("  test  ")).isEqualTo("test");
        assertThat(StringUtils.trimToEmpty("test")).isEqualTo("test");
        assertThat(StringUtils.trimToEmpty("")).isEqualTo("");
        assertThat(StringUtils.trimToEmpty("   ")).isEqualTo("");
        assertThat(StringUtils.trimToEmpty(null)).isEqualTo("");
    }

    @Test
    void shouldCapitalizeString() {
        assertThat(StringUtils.capitalize("test")).isEqualTo("Test");
        assertThat(StringUtils.capitalize("TEST")).isEqualTo("TEST");
        assertThat(StringUtils.capitalize("")).isEqualTo("");
        assertThat(StringUtils.capitalize(null)).isNull();
        assertThat(StringUtils.capitalize("t")).isEqualTo("T");
    }

    @Test
    void shouldConvertToCamelCase() {
        assertThat(StringUtils.toCamelCase("hello world")).isEqualTo("helloWorld");
        assertThat(StringUtils.toCamelCase("hello_world")).isEqualTo("helloWorld");
        assertThat(StringUtils.toCamelCase("hello-world")).isEqualTo("helloWorld");
        assertThat(StringUtils.toCamelCase("")).isEqualTo("");
        assertThat(StringUtils.toCamelCase(null)).isNull();
    }

    @Test
    void shouldConvertToSnakeCase() {
        assertThat(StringUtils.toSnakeCase("helloWorld")).isEqualTo("hello_world");
        assertThat(StringUtils.toSnakeCase("hello world")).isEqualTo("hello_world");
        assertThat(StringUtils.toSnakeCase("hello-world")).isEqualTo("hello_world");
        assertThat(StringUtils.toSnakeCase("")).isEqualTo("");
        assertThat(StringUtils.toSnakeCase(null)).isNull();
    }

    @Test
    void shouldConvertToKebabCase() {
        assertThat(StringUtils.toKebabCase("helloWorld")).isEqualTo("hello-world");
        assertThat(StringUtils.toKebabCase("hello world")).isEqualTo("hello-world");
        assertThat(StringUtils.toKebabCase("hello_world")).isEqualTo("hello-world");
        assertThat(StringUtils.toKebabCase("")).isEqualTo("");
        assertThat(StringUtils.toKebabCase(null)).isNull();
    }

    @Test
    void shouldTruncateString() {
        assertThat(StringUtils.truncate("hello world", 5)).isEqualTo("hello");
        assertThat(StringUtils.truncate("hello", 10)).isEqualTo("hello");
        assertThat(StringUtils.truncate("", 5)).isEqualTo("");
        assertThat(StringUtils.truncate(null, 5)).isNull();
    }

    @Test
    void shouldTruncateWithEllipsis() {
        assertThat(StringUtils.truncateWithEllipsis("hello world", 8)).isEqualTo("hello...");
        assertThat(StringUtils.truncateWithEllipsis("hello", 10)).isEqualTo("hello");
        assertThat(StringUtils.truncateWithEllipsis("", 5)).isEqualTo("");
        assertThat(StringUtils.truncateWithEllipsis(null, 5)).isNull();
    }

    @Test
    void shouldJoinStrings() {
        String[] array = {"a", "b", "c"};
        assertThat(StringUtils.join(array, ",")).isEqualTo("a,b,c");
        assertThat(StringUtils.join(array, " - ")).isEqualTo("a - b - c");
        assertThat(StringUtils.join(new String[]{}, ",")).isEqualTo("");
        assertThat(StringUtils.join(new String[]{"single"}, ",")).isEqualTo("single");
    }

    @Test
    void shouldValidateEmail() {
        assertThat(StringUtils.isValidEmail("test@example.com")).isTrue();
        assertThat(StringUtils.isValidEmail("user.name+tag@domain.co.uk")).isTrue();
        assertThat(StringUtils.isValidEmail("invalid-email")).isFalse();
        assertThat(StringUtils.isValidEmail("@domain.com")).isFalse();
        assertThat(StringUtils.isValidEmail("")).isFalse();
        assertThat(StringUtils.isValidEmail(null)).isFalse();
    }

    @Test
    void shouldValidatePhone() {
        assertThat(StringUtils.isValidPhone("+1234567890")).isTrue();
        assertThat(StringUtils.isValidPhone("1234567890")).isTrue();
        assertThat(StringUtils.isValidPhone("+12345678901234")).isTrue(); // 14 digits after +
        assertThat(StringUtils.isValidPhone("12")).isTrue(); // Minimum valid: 2 digits
        assertThat(StringUtils.isValidPhone("abc123")).isFalse();
        assertThat(StringUtils.isValidPhone("")).isFalse();
        assertThat(StringUtils.isValidPhone(null)).isFalse();
    }

    @Test
    void shouldCheckAlphanumeric() {
        assertThat(StringUtils.isAlphanumeric("abc123")).isTrue();
        assertThat(StringUtils.isAlphanumeric("ABC")).isTrue();
        assertThat(StringUtils.isAlphanumeric("123")).isTrue();
        assertThat(StringUtils.isAlphanumeric("abc-123")).isFalse();
        assertThat(StringUtils.isAlphanumeric("abc 123")).isFalse();
        assertThat(StringUtils.isAlphanumeric("")).isFalse();
        assertThat(StringUtils.isAlphanumeric(null)).isFalse();
    }

    @Test
    void shouldMaskString() {
        assertThat(StringUtils.mask("1234567890", 2, '*')).isEqualTo("12******90");
        assertThat(StringUtils.mask("short", 2, '*')).isEqualTo("sh*rt"); // 5 chars, 2 visible each side = 1 masked
        assertThat(StringUtils.mask("", 2, '*')).isEqualTo("");
        assertThat(StringUtils.mask(null, 2, '*')).isNull();
    }

    @Test
    void shouldMaskEmail() {
        assertThat(StringUtils.maskEmail("test@example.com")).isEqualTo("test@example.com");
        assertThat(StringUtils.maskEmail("a@example.com")).isEqualTo("a@example.com");
        assertThat(StringUtils.maskEmail("ab@example.com")).isEqualTo("ab@example.com");
        assertThat(StringUtils.maskEmail("invalid-email")).isEqualTo("invalid-email");
        assertThat(StringUtils.maskEmail("")).isEqualTo("");
        assertThat(StringUtils.maskEmail(null)).isNull();
    }

    @Test
    void shouldRemoveWhitespace() {
        assertThat(StringUtils.removeWhitespace("hello world")).isEqualTo("helloworld");
        assertThat(StringUtils.removeWhitespace("  test  ")).isEqualTo("test");
        assertThat(StringUtils.removeWhitespace("")).isEqualTo("");
        assertThat(StringUtils.removeWhitespace(null)).isNull();
    }

    @Test
    void shouldNormalizeWhitespace() {
        assertThat(StringUtils.normalizeWhitespace("hello    world")).isEqualTo("hello world");
        assertThat(StringUtils.normalizeWhitespace("  test  ")).isEqualTo("test");
        assertThat(StringUtils.normalizeWhitespace("")).isEqualTo("");
        assertThat(StringUtils.normalizeWhitespace(null)).isNull();
    }

    @Test
    void shouldCheckContainsIgnoreCase() {
        assertThat(StringUtils.containsIgnoreCase("Hello World", "WORLD")).isTrue();
        assertThat(StringUtils.containsIgnoreCase("Hello World", "missing")).isFalse();
        assertThat(StringUtils.containsIgnoreCase(null, "test")).isFalse();
        assertThat(StringUtils.containsIgnoreCase("test", null)).isFalse();
    }

    @Test
    void shouldReturnDefaultIfEmpty() {
        assertThat(StringUtils.defaultIfEmpty("", "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfEmpty(null, "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfEmpty("test", "default")).isEqualTo("test");
        assertThat(StringUtils.defaultIfEmpty(" ", "default")).isEqualTo(" ");
    }
}