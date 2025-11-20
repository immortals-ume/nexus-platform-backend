package com.immortals.notificationservice.service.impl;

import com.immortals.notificationservice.service.EmailService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Production-grade email service with circuit breaker, retry, rate limiting, and async support
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final RateLimiter emailRateLimiter;

    @Override
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendEmailFallback")
    @Retry(name = "emailService")
    @TimeLimiter(name = "emailService")
    public boolean sendEmail(String to, String subject, String body) {
        return emailRateLimiter.executeSupplier(() -> {
            try {
                log.debug("Sending email to: {}, subject: {}", to, subject);
                
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                
                mailSender.send(message);
                
                log.info("Email sent successfully to: {}", to);
                return true;
                
            } catch (MailException e) {
                log.error("Failed to send email to: {}, error: {}", to, e.getMessage());
                throw e;
            }
        });
    }

    @Override
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendHtmlEmailFallback")
    @Retry(name = "emailService")
    @TimeLimiter(name = "emailService")
    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        return emailRateLimiter.executeSupplier(() -> {
            try {
                log.debug("Sending HTML email to: {}, subject: {}", to, subject);
                
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                
                mailSender.send(mimeMessage);
                
                log.info("HTML email sent successfully to: {}", to);
                return true;
                
            } catch (MessagingException | MailException e) {
                log.error("Failed to send HTML email to: {}, error: {}", to, e.getMessage());
                throw new RuntimeException("Failed to send HTML email", e);
            }
        });
    }
    
    /**
     * Async email sending for non-blocking operations
     */
    @Async("notificationExecutor")
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String body) {
        return CompletableFuture.completedFuture(sendEmail(to, subject, body));
    }
    
    /**
     * Async HTML email sending for non-blocking operations
     */
    @Async("notificationExecutor")
    public CompletableFuture<Boolean> sendHtmlEmailAsync(String to, String subject, String htmlBody) {
        return CompletableFuture.completedFuture(sendHtmlEmail(to, subject, htmlBody));
    }

    /**
     * Fallback method for sendEmail when circuit breaker is open
     */
    private boolean sendEmailFallback(String to, String subject, String body, Exception e) {
        log.error("Email service circuit breaker activated. Failed to send email to: {}. Error: {}", 
                  to, e.getMessage());
        return false;
    }

    /**
     * Fallback method for sendHtmlEmail when circuit breaker is open
     */
    private boolean sendHtmlEmailFallback(String to, String subject, String htmlBody, Exception e) {
        log.error("Email service circuit breaker activated. Failed to send HTML email to: {}. Error: {}", 
                  to, e.getMessage());
        return false;
    }
}
