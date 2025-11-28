package com.example.discovery.metrics;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component for collecting and exposing Eureka Server metrics
 * Provides methods to retrieve current registry state and statistics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EurekaMetrics {

    private final EurekaServerContext eurekaServerContext;

    /**
     * Gets the total number of registered instances across all applications
     */
    public int getRegisteredInstancesCount() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            List<Application> applications = registry.getSortedApplications();
            
            int count = applications.stream()
                .mapToInt(app -> app.getInstances().size())
                .sum();
            
            log.debug("Total registered instances: {}", count);
            return count;
            
        } catch (Exception e) {
            log.error("Error getting registered instances count", e);
            return 0;
        }
    }

    /**
     * Gets the number of instances with UP status
     */
    public int getUpInstancesCount() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            List<Application> applications = registry.getSortedApplications();
            
            long count = applications.stream()
                .flatMap(app -> app.getInstances().stream())
                .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)
                .count();
            
            log.debug("UP instances: {}", count);
            return (int) count;
            
        } catch (Exception e) {
            log.error("Error getting UP instances count", e);
            return 0;
        }
    }

    /**
     * Gets the number of instances with DOWN status
     */
    public int getDownInstancesCount() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            List<Application> applications = registry.getSortedApplications();
            
            long count = applications.stream()
                .flatMap(app -> app.getInstances().stream())
                .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.DOWN)
                .count();
            
            log.debug("DOWN instances: {}", count);
            return (int) count;
            
        } catch (Exception e) {
            log.error("Error getting DOWN instances count", e);
            return 0;
        }
    }

    /**
     * Gets the total number of registered applications
     */
    public int getApplicationsCount() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            int count = registry.getSortedApplications().size();
            
            log.debug("Total registered applications: {}", count);
            return count;
            
        } catch (Exception e) {
            log.error("Error getting applications count", e);
            return 0;
        }
    }

    /**
     * Gets the number of available peer replicas
     */
    public int getAvailableReplicasCount() {
        try {
            int count = eurekaServerContext.getPeerEurekaNodes().getPeerEurekaNodes().size();
            
            log.debug("Available replicas: {}", count);
            return count;
            
        } catch (Exception e) {
            log.error("Error getting available replicas count", e);
            return 0;
        }
    }

    /**
     * Gets instance count per application
     */
    public Map<String, Integer> getInstanceCountByApplication() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            List<Application> applications = registry.getSortedApplications();
            
            Map<String, Integer> counts = new HashMap<>();
            for (Application app : applications) {
                counts.put(app.getName(), app.getInstances().size());
            }
            
            log.debug("Instance count by application: {}", counts);
            return counts;
            
        } catch (Exception e) {
            log.error("Error getting instance count by application", e);
            return new HashMap<>();
        }
    }

    /**
     * Gets instance count by status
     */
    public Map<String, Integer> getInstanceCountByStatus() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            List<Application> applications = registry.getSortedApplications();
            
            Map<String, Integer> counts = new HashMap<>();
            
            applications.stream()
                .flatMap(app -> app.getInstances().stream())
                .forEach(instance -> {
                    String status = instance.getStatus().name();
                    counts.put(status, counts.getOrDefault(status, 0) + 1);
                });
            
            log.debug("Instance count by status: {}", counts);
            return counts;
            
        } catch (Exception e) {
            log.error("Error getting instance count by status", e);
            return new HashMap<>();
        }
    }

    /**
     * Checks if self-preservation mode is enabled
     */
    public boolean isSelfPreservationModeEnabled() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            boolean enabled = registry.isSelfPreservationModeEnabled();
            
            log.debug("Self-preservation mode enabled: {}", enabled);
            return enabled;
            
        } catch (Exception e) {
            log.error("Error checking self-preservation mode", e);
            return false;
        }
    }

    /**
     * Gets the number of renews in the last minute
     */
    public long getRenewsLastMinute() {
        try {
            PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
            long renews = registry.getNumOfRenewsInLastMin();
            
            log.debug("Renews in last minute: {}", renews);
            return renews;
            
        } catch (Exception e) {
            log.error("Error getting renews last minute", e);
            return 0;
        }
    }

    /**
     * Gets comprehensive registry statistics
     */
    public RegistryStatistics getRegistryStatistics() {
        return RegistryStatistics.builder()
            .totalInstances(getRegisteredInstancesCount())
            .upInstances(getUpInstancesCount())
            .downInstances(getDownInstancesCount())
            .totalApplications(getApplicationsCount())
            .availableReplicas(getAvailableReplicasCount())
            .selfPreservationMode(isSelfPreservationModeEnabled())
            .renewsLastMinute(getRenewsLastMinute())
            .instancesByApplication(getInstanceCountByApplication())
            .instancesByStatus(getInstanceCountByStatus())
            .build();
    }
}
