package com.immortals.authapp.service;

import com.immortals.platform.domain.auth.dto.LoginDto;
import com.immortals.platform.domain.auth.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    LoginResponse login(LoginDto loginDto);
    LoginResponse generateRefreshToken(String username);
    void logout(HttpServletRequest request);
}