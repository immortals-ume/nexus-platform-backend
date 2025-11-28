package com.example.discovery.listener;

import com.example.discovery.validation.ServiceRegistrationException;
import com.example.discovery.validation.ServiceRegistrationValidator;
import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for Eureka registration events
 * Validates service registrations and logs registration activity
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEventListener {

    private final ServiceRegistrationValidator validator;

    /**
     * Handles service registration events
     * Validates the registration and logs the event
     */
    @EventListener
    public void handleRegistration(EurekaInstanceRegisteredEvent event) {
        InstanceInfo instanceInfo = event.getInstanceInfo();
        
        try {
            log.info("Service registration event received for: {} ({})",
                instanceInfo.getAppName(), instanceInfo.getInstanceId());
            
            validator.validate(instanceInfo);
            
            log.info("Service registration validated and accepted: {} at {}:{}",
                instanceInfo.getAppName(),
                instanceInfo.getHostName(),
                instanceInfo.getPort());
                
        } catch (ServiceRegistrationException e) {
            log.error("Service registration validation failed for {}: {}",
                instanceInfo.getAppName(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during service registration validation for {}: {}",
                instanceInfo.getAppName(), e.getMessage(), e);
        }
    }
}
