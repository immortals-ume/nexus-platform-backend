package com.immortals.notificationservice.infrastructure.provider;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.port.NotificationProvider;
import com.immortals.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email notification provider implementation (Infrastructure Layer)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {
    
    private final EmailService emailService;
    
    @Override
    public String getProviderId() {
        return "EMAIL_SMTP";
    }
    
    @Override
    public int getPriority() {
        return 1; // Highest priority
    }
    
    @Override
    public boolean send(Notification notification) {
        try {
            if (notification.getHtmlContent() != null && !notification.getHtmlContent().isEmpty()) {
                return emailService.sendHtmlEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getHtmlContent()
                );
            } else {
                return emailService.sendEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getMessage()
                );
            }
        } catch (Exception e) {
            log.error("Error sending email notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean supports(Notification.NotificationType type) {
        return type == Notification.NotificationType.EMAIL;
    }
}
