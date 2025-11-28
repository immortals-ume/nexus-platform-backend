package com.immortals.platform.domain.util;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Immutable physical address with validation and normalization.
 * Supports US ZIP code format and 2-letter ISO country codes.
 */
public record Address(
    @NotBlank @Size(max = 255) String street,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "Invalid ZIP code format") String zipCode,
    @NotBlank @Size(min = 2, max = 2) String country
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be null or blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be null or blank");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State cannot be null or blank");
        }
        if (zipCode == null || zipCode.isBlank()) {
            throw new IllegalArgumentException("ZIP code cannot be null or blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be null or blank");
        }
        if (country.length() != 2) {
            throw new IllegalArgumentException("Country must be a 2-letter ISO code");
        }

        street = street.trim();
        city = city.trim();
        state = state.trim();
        zipCode = zipCode.trim();
        country = country.trim().toUpperCase();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFullAddress() {
        return String.format("%s, %s, %s %s, %s", street, city, state, zipCode, country);
    }

    public boolean isUSAddress() {
        return "US".equals(country);
    }

    public static class Builder {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Address build() {
            return new Address(street, city, state, zipCode, country);
        }
    }

    @Override
    public String toString() {
        return getFullAddress();
    }
}
