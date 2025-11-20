package com.immortals.otpservice.controller;

import com.immortals.otpservice.service.OtpService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller for OTP operations.
 * Demonstrates cache-starter integration with metrics and monitoring.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    /**
     * Generate and send OTP to mobile number.
     * 
     * @param request OTP generation request
     * @return Success response
     */
    @PostMapping("/generate")
    public ResponseEntity<OtpResponse> generateOtp(@Valid @RequestBody OtpRequest request) {
        log.info("Received OTP generation request for mobile: {}", request.getMobile());
        
        try {
            otpService.sendOtp(request.getMobile());
            return ResponseEntity.ok(OtpResponse.success("OTP sent successfully"));
        } catch (Exception e) {
            log.error("Failed to generate OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(OtpResponse.error(e.getMessage()));
        }
    }

    /**
     * Verify OTP for mobile number.
     * 
     * @param request OTP verification request
     * @return Success or error response
     */
    @PostMapping("/verify")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        log.info("Received OTP verification request for mobile: {}", request.getMobile());
        
        try {
            otpService.verifyOtp(request.getMobile(), request.getOtp());
            return ResponseEntity.ok(OtpResponse.success("OTP verified successfully"));
        } catch (Exception e) {
            log.error("Failed to verify OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(OtpResponse.error(e.getMessage()));
        }
    }

    /**
     * Resend OTP to mobile number.
     * 
     * @param request OTP resend request
     * @return Success response
     */
    @PostMapping("/resend")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody OtpRequest request) {
        log.info("Received OTP resend request for mobile: {}", request.getMobile());
        
        try {
            otpService.resendOtp(request.getMobile());
            return ResponseEntity.ok(OtpResponse.success("OTP resent successfully"));
        } catch (Exception e) {
            log.error("Failed to resend OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(OtpResponse.error(e.getMessage()));
        }
    }

    /**
     * Check if OTP is expired for mobile number.
     * 
     * @param mobile Mobile number
     * @return Expiry status
     */
    @GetMapping("/status/{mobile}")
    public ResponseEntity<OtpStatusResponse> checkOtpStatus(@PathVariable String mobile) {
        log.info("Checking OTP status for mobile: {}", mobile);
        
        boolean expired = otpService.isOtpExpired(mobile);
        return ResponseEntity.ok(new OtpStatusResponse(mobile, !expired, expired));
    }

    // DTOs

    @Data
    public static class OtpRequest {
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid mobile number format")
        private String mobile;
    }

    @Data
    public static class OtpVerifyRequest {
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid mobile number format")
        private String mobile;

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        private String otp;
    }

    @Data
    public static class OtpResponse {
        private boolean success;
        private String message;

        public static OtpResponse success(String message) {
            OtpResponse response = new OtpResponse();
            response.setSuccess(true);
            response.setMessage(message);
            return response;
        }

        public static OtpResponse error(String message) {
            OtpResponse response = new OtpResponse();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }

    @Data
    public static class OtpStatusResponse {
        private String mobile;
        private boolean active;
        private boolean expired;

        public OtpStatusResponse(String mobile, boolean active, boolean expired) {
            this.mobile = mobile;
            this.active = active;
            this.expired = expired;
        }
    }
}
