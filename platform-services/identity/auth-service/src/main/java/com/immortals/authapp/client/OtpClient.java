package com.immortals.authapp.client;

/**
 * Client interface for external OTP service communication.
 * This interface defines the contract for OTP service operations.
 */
public interface OtpClient {
    
    /**
     * Send OTP to the specified mobile number via external OTP service.
     * 
     * @param mobile the mobile number to send OTP to
     * @return true if OTP was sent successfully, false otherwise
     */
    boolean sendOtp(String mobile);
    
    /**
     * Verify OTP with the external OTP service.
     * 
     * @param mobile the mobile number
     * @param otp the OTP code to verify
     * @return true if OTP is valid, false otherwise
     */
    boolean verifyOtp(String mobile, String otp);
    
    /**
     * Resend OTP to the specified mobile number.
     * 
     * @param mobile the mobile number
     * @return true if OTP was resent successfully, false otherwise
     */
    boolean resendOtp(String mobile);
}
