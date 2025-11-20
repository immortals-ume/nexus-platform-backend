package com.immortals.authapp.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {
    FORM_LOGIN("formLogin");

    private final String provider;
}