package com.immortals.authapp.controller;

import com.immortals.authapp.service.GuestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.immortals.authapp.constants.AuthAppConstant.HEADER_STRING;
import static com.immortals.authapp.constants.AuthAppConstant.TOKEN_PREFIX;

@RestController
@RequestMapping("/api/v1/guest")
@RequiredArgsConstructor
@Slf4j
public class GuestController {
    private final GuestService guestService;

    @GetMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> loginWithGuestCredentials() {

        HttpHeaders responseHeaders = new HttpHeaders();
        String guestToken = guestService.generateGuestLogin();
        responseHeaders.set(HEADER_STRING, TOKEN_PREFIX + guestToken);

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body("Guest Token generate successfully");
    }

}
