package com.immortals.otpservice.controller;

import com.immortals.authapp.helper.ValidateCredentials;
import com.immortals.authapp.model.dto.LoginDto;
import com.immortals.authapp.model.dto.LoginResponse;
import com.immortals.authapp.service.AuthService;
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

    @PreAuthorize(" hasRole('GUEST') ")
    @GetMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginDto loginDto) {
        validateCredentials.validateLoginDto(loginDto);
        HttpHeaders responseHeaders = new HttpHeaders();
        LoginResponse loginResponse= authService.login(loginDto);
        responseHeaders.set(HEADER_STRING, TOKEN_PREFIX + loginResponse.accessToken());

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(loginResponse);
    }

    @PreAuthorize(" hasRole('GUEST') ")
    @GetMapping(value = "/refresh/{username}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = org.springframework.http.HttpStatus.OK)
    public String generateRefreshToken(@PathVariable @NotNull String username) {
       return  authService.generateRefreshToken(username);
    }

    @PreAuthorize(" hasRole('GUEST') ")
    @DeleteMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout();
    }
}
