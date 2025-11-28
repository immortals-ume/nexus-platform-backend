package com.example.discovery.validation;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for service registration requests
 * Validates required fields and data integrity for Eureka service registrations
 */
@Slf4j
@Component
public class ServiceRegistrationValidator {

    /**
     * Validates a service registration instance
     * 
     * @param instanceInfo the instance to validate
     * @throws ServiceRegistrationException if validation fails
     */
    public void validate(InstanceInfo instanceInfo) {
        log.debug("Validating service registration for instance: {}",
            instanceInfo != null ? instanceInfo.getInstanceId() : "null");
        
        if (instanceInfo == null) {
            throw new ServiceRegistrationException("Instance information cannot be null");
        }
        
        validateServiceId(instanceInfo.getAppName());
        
        validateHost(instanceInfo.getHostName());
        
        validatePort(instanceInfo.getPort());
        
        validateInstanceId(instanceInfo.getInstanceId());
        
        log.info("Service registration validation successful for service: {}, instance: {}",
            instanceInfo.getAppName(), instanceInfo.getInstanceId());
    }
    
    /**
     * Validates the service ID (application name)
     */
    private void validateServiceId(String serviceId) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            throw new ServiceRegistrationException("Service ID (appName) is required and cannot be empty");
        }
        
        if (serviceId.length() < 3) {
            throw new ServiceRegistrationException(
                "Service ID must be at least 3 characters long, got: " + serviceId);
        }
        
        if (serviceId.length() > 100) {
            throw new ServiceRegistrationException(
                "Service ID must not exceed 100 characters, got length: " + serviceId.length());
        }
        
        if (!serviceId.matches("^[a-zA-Z0-9][a-zA-Z0-9-_]*$")) {
            throw new ServiceRegistrationException(
                "Service ID must contain only alphanumeric characters, hyphens, and underscores, " +
                "and must start with an alphanumeric character. Got: " + serviceId);
        }
    }
    
    /**
     * Validates the host name
     */
    private void validateHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new ServiceRegistrationException("Host name is required and cannot be empty");
        }
        
        if (host.length() > 255) {
            throw new ServiceRegistrationException(
                "Host name must not exceed 255 characters, got length: " + host.length());
        }
        
        if (!host.matches("^[a-zA-Z0-9][a-zA-Z0-9.-]*[a-zA-Z0-9]$") &&
            !host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            throw new ServiceRegistrationException(
                "Host name must be a valid hostname or IP address. Got: " + host);
        }
    }
    
    /**
     * Validates the port number
     */
    private void validatePort(int port) {
        if (port <= 0) {
            throw new ServiceRegistrationException(
                "Port must be a positive number, got: " + port);
        }
        
        if (port > 65535) {
            throw new ServiceRegistrationException(
                "Port must not exceed 65535, got: " + port);
        }
        
        if (port < 1024) {
            log.warn("Service registering with privileged port: {}", port);
        }
    }
    
    /**
     * Validates the instance ID
     */
    private void validateInstanceId(String instanceId) {
        if (instanceId == null || instanceId.trim().isEmpty()) {
            throw new ServiceRegistrationException("Instance ID is required and cannot be empty");
        }
        
        if (instanceId.length() > 255) {
            throw new ServiceRegistrationException(
                "Instance ID must not exceed 255 characters, got length: " + instanceId.length());
        }
    }
}
