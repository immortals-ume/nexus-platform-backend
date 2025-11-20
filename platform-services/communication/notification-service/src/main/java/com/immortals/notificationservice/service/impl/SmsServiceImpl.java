package com.immortals.notificationservice.service.impl;

import com.immortals.notificationservice.service.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

/**
 * Production-grade SMS service with circuit breaker, retry, rate limiting, and async support
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final RateLimiter smsRateLimiter;

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio credentials not configured. SMS functionality will be disabled.");
        }
    }

    @Override
    @CircuitBreaker(name = "smsService", fallbackMethod = "sendSmsFallback")
    @Retry(name = "smsService")
    @TimeLimiter(name = "smsService")
    public boolean sendSms(String to, String messageBody) {
        return smsRateLimiter.executeSupplier(() -> {
            try {
                if (accountSid == null || accountSid.isEmpty()) {
                    log.warn("Twilio not configured. Cannot send SMS to: {}", to);
                    return false;
                }

                log.debug("Sending SMS to: {}", to);
                
                Message message = Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(fromPhoneNumber),
                        messageBody
                ).create();
                
                log.info("SMS sent successfully to: {}. Message SID: {}", to, message.getSid());
                return true;
                
            } catch (Exception e) {
                log.error("Failed to send SMS to: {}, error: {}", to, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Async SMS sending for non-blocking operations
     */
    @Async("notificationExecutor")
    public CompletableFuture<Boolean> sendSmsAsync(String to, String messageBody) {
        return CompletableFuture.completedFuture(sendSms(to, messageBody));
    }

    /**
     * Fallback method for sendSms when circuit breaker is open
     */
    private boolean sendSmsFallback(String to, String messageBody, Exception e) {
        log.error("SMS service circuit breaker activated. Failed to send SMS to: {}. Error: {}", 
                  to, e.getMessage());
        return false;
    }
}
