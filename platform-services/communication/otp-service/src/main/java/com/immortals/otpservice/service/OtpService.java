package com.immortals.otpservice.service;

public interface OtpService {
    void sendOtp(String mobile);

    void verifyOtp(String mobile, String otp);

    void resendOtp(String mobile);

    boolean isOtpExpired(String mobile);
}
