package com.immortals.authapp.controller;

import com.immortals.authapp.helper.ValidateCredentials;
import com.immortals.platform.common.response.ApiResponse;
import com.immortals.platform.domain.auth.dto.LoginDto;
import com.immortals.platform.domain.auth.dto.LoginResponse;

import com.immortals.authapp.service.AuthService;
import com.immortals.authapp.service.impl.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.immortals.platform.domain.auth.constants.AuthAppConstant.HEADER_STRING;
import static com.immortals.platform.domain.auth.constants.AuthAppConstant.TOKEN_PREFIX;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ValidateCredentials validateCredentials;
    private final CookieUtils cookieUtils;

    @PreAuthorize(" hasRole('GUEST') ")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginDto loginDto) {
        String correlationId = UUID.randomUUID().toString();
        
        validateCredentials.validateLoginDto(loginDto);
        LoginResponse loginResponse = authService.login(loginDto);
        
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HEADER_STRING, TOKEN_PREFIX + loginResponse.accessToken());
        responseHeaders.set("X-Correlation-ID", correlationId);
        
        ApiResponse<LoginResponse> response = ApiResponse.success(
            loginResponse, 
            "Login successful", 
            correlationId
        );
        
        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(response);
    }



    @PreAuthorize(" hasRole('USER') || hasRole('ADMIN') ")
    @PostMapping(value = "/refresh/{username}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<LoginResponse>> generateRefreshToken(
            @PathVariable @NotNull String username, 
            HttpServletResponse response) {
        
        String correlationId = UUID.randomUUID().toString();
        
        LoginResponse loginResponse = authService.generateRefreshToken(username);
        cookieUtils.setRefreshTokenCookie(response, loginResponse.refreshToken());
        
        response.setHeader("X-Correlation-ID", correlationId);
        
        ApiResponse<LoginResponse> apiResponse = ApiResponse.success(
            loginResponse,
            "Refresh token generated successfully",
            correlationId
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    @PreAuthorize(" hasRole('USER') || hasRole('ADMIN') ")
    @DeleteMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String correlationId = UUID.randomUUID().toString();
        
        authService.logout(request);
        cookieUtils.clearRefreshTokenCookie(response);
        
        response.setHeader("X-Correlation-ID", correlationId);
        
        ApiResponse<Void> apiResponse = ApiResponse.success(
            null,
            "Logout successful",
            correlationId
        );
        
        return ResponseEntity.ok(apiResponse);
    }
}
