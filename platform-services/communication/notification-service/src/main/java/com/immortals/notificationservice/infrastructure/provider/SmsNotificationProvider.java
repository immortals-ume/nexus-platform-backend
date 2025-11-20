package com.immortals.notificationservice.infrastructure.provider;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.port.NotificationProvider;
import com.immortals.notificationservice.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS notification provider implementation (Infrastructure Layer)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationProvider implements NotificationProvider {
    
    private final SmsService smsService;
    
    @Override
    public String getProviderId() {
        return "SMS_TWILIO";
    }
    
    @Override
    public int getPriority() {
        return 1; // Highest priority
    }
    
    @Override
    public boolean send(Notification notification) {
        try {
            return smsService.sendSms(
                    notification.getRecipient(),
                    notification.getMessage()
            );
        } catch (Exception e) {
            log.error("Error sending SMS notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean supports(Notification.NotificationType type) {
        return type == Notification.NotificationType.SMS;
    }
}
