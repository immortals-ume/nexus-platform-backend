package com.example.discovery.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data class representing Eureka registry statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryStatistics {
    private int totalInstances;
    private int upInstances;
    private int downInstances;
    private int totalApplications;
    private int availableReplicas;
    private boolean selfPreservationMode;
    private long renewsLastMinute;
    private Map<String, Integer> instancesByApplication;
    private Map<String, Integer> instancesByStatus;
}
