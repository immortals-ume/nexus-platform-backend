package com.immortals.authapp.utils;


import com.immortals.platform.domain.properties.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

import static com.immortals.authapp.constants.CacheConstants.REFRESH_TOKEN_HASH_KEY;

@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final CookieProperties props;

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_HASH_KEY, token);
        cookie.setHttpOnly(Boolean.TRUE);
        cookie.setSecure(props.getSecure());
        cookie.setPath("/");
        cookie.setMaxAge(props.getMaxAge());
        response.addCookie(cookie);
    }

    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_HASH_KEY.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_HASH_KEY, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Remove it
        response.addCookie(cookie);
    }
}
