package com.immortals.authapp.model.dto;

public record ResetCredentials(String userName,
                               String password,
                               String reTypePassword) {
}
