package com.immortals.otpservice.helper;

import com.immortals.authapp.constants.AuthAppConstant;
import com.immortals.authapp.model.dto.LoginDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateCredentials {

    public void validateLoginDto(LoginDto dto) {

        if (!StringUtils.hasText(dto.username()) || dto.username()
                .isEmpty() || dto.username().length() > 16) {
            throw new IllegalArgumentException("Username must be between 1 and 16 characters");
        }


        if (!StringUtils.hasText(dto.password()) || !Pattern.matches(AuthAppConstant.PASSWORD_REGEX, dto.password())) {
            throw new IllegalArgumentException("Password is not in correct format");
        }


    }

}
