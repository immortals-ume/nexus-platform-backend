package com.immortals.platform.domain.auth.dto;

public record ResetCredentials(String userName,
                               String password,
                               String reTypePassword) {
}
