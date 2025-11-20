package com.immortals.notificationservice.infrastructure.provider;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.model.ProviderHealth;
import com.immortals.notificationservice.domain.port.NotificationProvider;
import com.immortals.notificationservice.service.ProviderHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Failover strategy - tries providers in order until one succeeds
 * Prioritizes healthy providers based on success rates
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FailoverProviderStrategy implements ProviderStrategy {
    
    private final ProviderHealthService providerHealthService;
    
    @Override
    public NotificationProvider selectProvider(
            Notification notification,
            List<NotificationProvider> availableProviders) {
        
        // Filter providers that support the notification type
        var supportedProviders = availableProviders.stream()
                .filter(p -> p.supports(notification.getType()))
                .toList();
        
        if (supportedProviders.isEmpty()) {
            throw new IllegalStateException("No providers available for type: " + notification.getType());
        }
        
        // Sort by health status (healthy first)
        var sortedProviders = supportedProviders.stream()
                .sorted((p1, p2) -> {
                    var health1 = providerHealthService.getHealth(p1.getProviderId(), notification.getType());
                    var health2 = providerHealthService.getHealth(p2.getProviderId(), notification.getType());
                    return Double.compare(health2.getSuccessRate(), health1.getSuccessRate());
                })
                .toList();
        
        // Return the healthiest provider
        var selectedProvider = sortedProviders.get(0);
        log.info("Selected provider: {} for type: {}", 
                 selectedProvider.getProviderId(), notification.getType());
        
        return selectedProvider;
    }
    
    @Override
    public String getStrategyName() {
        return "FAILOVER";
    }
}
