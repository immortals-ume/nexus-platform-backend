package com.immortals.platform.domain.dto;

public record ResetCredentials(String userName,
                               String password,
                               String reTypePassword) {
}
