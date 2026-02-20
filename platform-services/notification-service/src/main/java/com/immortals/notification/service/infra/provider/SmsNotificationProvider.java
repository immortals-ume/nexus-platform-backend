package com.immortals.notification.service.infra.provider;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import com.immortals.notification.service.service.SmsService;
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
    private final com.immortals.notification.service.configuration.NotificationServiceProperties properties;
    
    @Override
    public String getProviderId() {
        return properties.getProviders().getSmsProviderId();
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
