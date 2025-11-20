package com.immortals.authapp.controller;

import com.immortals.authapp.helper.ValidateCredentials;
import com.immortals.platform.domain.dto.LoginDto;
import com.immortals.platform.domain.dto.LoginResponse;
import com.immortals.authapp.service.AuthService;
import com.immortals.authapp.utils.CookieUtils;
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

import static com.immortals.authapp.constants.AuthAppConstant.HEADER_STRING;
import static com.immortals.authapp.constants.AuthAppConstant.TOKEN_PREFIX;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ValidateCredentials validateCredentials;
    private final CookieUtils cookieUtils;

    @PreAuthorize(" hasRole('GUEST') ")
    @GetMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> login(@RequestBody @Valid LoginDto loginDto) {
        validateCredentials.validateLoginDto(loginDto);
        HttpHeaders responseHeaders = new HttpHeaders();
        LoginResponse loginResponse = authService.login(loginDto);
        responseHeaders.set(HEADER_STRING, TOKEN_PREFIX + loginResponse.accessToken());
        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(loginResponse.message());
    }

    @PreAuthorize(" hasRole('USER') && hasRole('ADMIN') ")
    @GetMapping(value = "/refresh/{username}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = org.springframework.http.HttpStatus.OK)
    public ResponseEntity<String> generateRefreshToken(@PathVariable @NotNull String username, HttpServletResponse response) {
        LoginResponse loginResponse = authService.generateRefreshToken(username);

        cookieUtils.setRefreshTokenCookie(response, loginResponse.refreshToken());
        return ResponseEntity.ok()
                .body(loginResponse.message());
    }

    @PreAuthorize(" hasRole('USER') && hasRole('ADMIN') ")
    @DeleteMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request);
        cookieUtils.clearRefreshTokenCookie(response);
    }
}
