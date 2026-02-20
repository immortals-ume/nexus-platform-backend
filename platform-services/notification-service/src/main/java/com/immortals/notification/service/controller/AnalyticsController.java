package com.immortals.notification.service.controller;

import com.immortals.platform.domain.notifications.dto.AnalyticsFilter;
import com.immortals.platform.domain.notifications.dto.AnalyticsMetrics;
import com.immortals.notification.service.service.NotificationAnalyticsService;
import com.immortals.platform.domain.dto.ApiResponse;
import com.immortals.platform.domain.notifications.domain.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API endpoints for notification analytics and reporting
 * Requirements: 10.2, 10.5
 */
@RestController
@RequestMapping("/api/v1/notifications/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Notification analytics and reporting APIs")
public class AnalyticsController {
    
    private final NotificationAnalyticsService analyticsService;
    
    /**
     * Get comprehensive analytics metrics with filtering support
     * Requirement 10.2: Query analytics by channel, provider, country, and time period
     */
    @GetMapping
    @Operation(summary = "Get analytics metrics", 
               description = "Get comprehensive analytics with optional filtering by channel, provider, country, and time period")
    public ResponseEntity<ApiResponse<AnalyticsMetrics>> getAnalytics(
            @Parameter(description = "Start date for analytics period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime startDate,
            
            @Parameter(description = "End date for analytics period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime endDate,
            
            @Parameter(description = "Filter by notification channel (EMAIL, SMS, WHATSAPP, PUSH_NOTIFICATION)")
            @RequestParam(required = false) 
            Notification.NotificationType channel,
            
            @Parameter(description = "Filter by provider ID")
            @RequestParam(required = false) 
            String providerId,
            
            @Parameter(description = "Filter by country code")
            @RequestParam(required = false) 
            String countryCode) {
        
        log.info("Getting analytics - startDate: {}, endDate: {}, channel: {}, providerId: {}, countryCode: {}", 
                 startDate, endDate, channel, providerId, countryCode);
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .channel(channel)
            .providerId(providerId)
            .countryCode(countryCode)
            .build();
        
        AnalyticsMetrics metrics = analyticsService.getAnalytics(filter);
        
        return ResponseEntity.ok(ApiResponse.success(metrics, "Analytics retrieved successfully"));
    }
    
    /**
     * Get metrics by channel
     */
    @GetMapping("/by-channel")
    @Operation(summary = "Get metrics by channel", 
               description = "Get aggregated metrics grouped by notification channel")
    public ResponseEntity<ApiResponse<Map<String, AnalyticsMetrics.ChannelMetrics>>> getMetricsByChannel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String countryCode) {
        
        log.info("Getting metrics by channel");
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .providerId(providerId)
            .countryCode(countryCode)
            .build();
        
        Map<String, AnalyticsMetrics.ChannelMetrics> metrics = analyticsService.getMetricsByChannel(filter);
        
        return ResponseEntity.ok(ApiResponse.success(metrics, "Channel metrics retrieved successfully"));
    }
    
    /**
     * Get metrics by provider
     */
    @GetMapping("/by-provider")
    @Operation(summary = "Get metrics by provider", 
               description = "Get aggregated metrics grouped by notification provider")
    public ResponseEntity<ApiResponse<Map<String, AnalyticsMetrics.ProviderMetrics>>> getMetricsByProvider(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Notification.NotificationType channel,
            @RequestParam(required = false) String countryCode) {
        
        log.info("Getting metrics by provider");
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .channel(channel)
            .countryCode(countryCode)
            .build();
        
        Map<String, AnalyticsMetrics.ProviderMetrics> metrics = analyticsService.getMetricsByProvider(filter);
        
        return ResponseEntity.ok(ApiResponse.success(metrics, "Provider metrics retrieved successfully"));
    }
    
    /**
     * Get metrics by country
     */
    @GetMapping("/by-country")
    @Operation(summary = "Get metrics by country", 
               description = "Get aggregated metrics grouped by country code")
    public ResponseEntity<ApiResponse<Map<String, AnalyticsMetrics.CountryMetrics>>> getMetricsByCountry(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Notification.NotificationType channel,
            @RequestParam(required = false) String providerId) {
        
        log.info("Getting metrics by country");
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .channel(channel)
            .providerId(providerId)
            .build();
        
        Map<String, AnalyticsMetrics.CountryMetrics> metrics = analyticsService.getMetricsByCountry(filter);
        
        return ResponseEntity.ok(ApiResponse.success(metrics, "Country metrics retrieved successfully"));
    }
    
    /**
     * Get failure reasons categorization
     * Requirement 10.3: Categorize failures by reason
     */
    @GetMapping("/failures")
    @Operation(summary = "Get failure categorization", 
               description = "Get failure counts categorized by reason (provider error, invalid recipient, rate limit, etc.)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFailureReasons(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Notification.NotificationType channel,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String countryCode) {
        
        log.info("Getting failure reasons");
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .channel(channel)
            .providerId(providerId)
            .countryCode(countryCode)
            .build();
        
        Map<String, Long> failureReasons = analyticsService.getFailureReasons(filter);
        
        return ResponseEntity.ok(ApiResponse.success(failureReasons, "Failure reasons retrieved successfully"));
    }
    
    /**
     * Get average delivery time by channel
     * Requirement 10.4: Calculate average delivery time per channel
     */
    @GetMapping("/delivery-time/by-channel")
    @Operation(summary = "Get average delivery time by channel", 
               description = "Get average delivery time in seconds for each channel")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getAverageDeliveryTimeByChannel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String countryCode) {
        
        log.info("Getting average delivery time by channel");
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .providerId(providerId)
            .countryCode(countryCode)
            .build();
        
        Map<String, Double> avgTimes = analyticsService.getAverageDeliveryTimeByChannel(filter);
        
        return ResponseEntity.ok(ApiResponse.success(avgTimes, "Average delivery times retrieved successfully"));
    }
    
    /**
     * Get average delivery time by provider
     * Requirement 10.4: Calculate average delivery time per provider
     */
    @GetMapping("/delivery-time/by-provider")
    @Operation(summary = "Get average delivery time by provider", 
               description = "Get average delivery time in seconds for each provider")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getAverageDeliveryTimeByProvider(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Notification.NotificationType channel,
            @RequestParam(required = false) String countryCode) {
        
        log.info("Getting average delivery time by provider");
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .channel(channel)
            .countryCode(countryCode)
            .build();
        
        Map<String, Double> avgTimes = analyticsService.getAverageDeliveryTimeByProvider(filter);
        
        return ResponseEntity.ok(ApiResponse.success(avgTimes, "Average delivery times retrieved successfully"));
    }
    
    /**
     * Export analytics data in CSV or JSON format
     * Requirement 10.5: Support CSV and JSON export formats
     */
    @GetMapping("/export")
    @Operation(summary = "Export analytics data", 
               description = "Export analytics data in CSV or JSON format")
    public ResponseEntity<String> exportAnalytics(
            @Parameter(description = "Export format: csv or json")
            @RequestParam(defaultValue = "json") String format,
            
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Notification.NotificationType channel,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String countryCode) {
        
        log.info("Exporting analytics in {} format", format);
        
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(startDate)
            .endDate(endDate)
            .channel(channel)
            .providerId(providerId)
            .countryCode(countryCode)
            .build();
        
        AnalyticsMetrics metrics = analyticsService.getAnalytics(filter);
        
        if ("csv".equalsIgnoreCase(format)) {
            String csv = convertToCsv(metrics);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", 
                "analytics_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
            return new ResponseEntity<>(csv, headers, HttpStatus.OK);
        } else {
            String json = convertToJson(metrics);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", 
                "analytics_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json");
            return new ResponseEntity<>(json, headers, HttpStatus.OK);
        }
    }
    
    /**
     * Convert analytics metrics to CSV format
     */
    private String convertToCsv(AnalyticsMetrics metrics) {
        StringBuilder csv = new StringBuilder();
        
        csv.append("Metric,Value\n");
        csv.append("Total Sent,").append(metrics.getTotalSent()).append("\n");
        csv.append("Total Delivered,").append(metrics.getTotalDelivered()).append("\n");
        csv.append("Total Failed,").append(metrics.getTotalFailed()).append("\n");
        csv.append("Total Read,").append(metrics.getTotalRead()).append("\n");
        csv.append("Delivery Rate (%),").append(String.format("%.2f", metrics.getDeliveryRate())).append("\n");
        csv.append("Failure Rate (%),").append(String.format("%.2f", metrics.getFailureRate())).append("\n");
        csv.append("Read Rate (%),").append(String.format("%.2f", metrics.getReadRate())).append("\n");
        csv.append("Avg Delivery Time (s),").append(String.format("%.2f", metrics.getAverageDeliveryTimeSeconds())).append("\n");
        csv.append("\n");

        if (metrics.getChannelMetrics() != null && !metrics.getChannelMetrics().isEmpty()) {
            csv.append("Channel Metrics\n");
            csv.append("Channel,Sent,Delivered,Failed,Read,Delivery Rate (%),Failure Rate (%),Avg Delivery Time (s)\n");
            metrics.getChannelMetrics().forEach((channel, channelMetrics) -> {
                csv.append(channel).append(",")
                   .append(channelMetrics.getSent()).append(",")
                   .append(channelMetrics.getDelivered()).append(",")
                   .append(channelMetrics.getFailed()).append(",")
                   .append(channelMetrics.getRead()).append(",")
                   .append(String.format("%.2f", channelMetrics.getDeliveryRate())).append(",")
                   .append(String.format("%.2f", channelMetrics.getFailureRate())).append(",")
                   .append(String.format("%.2f", channelMetrics.getAverageDeliveryTimeSeconds())).append("\n");
            });
            csv.append("\n");
        }
        
        if (metrics.getProviderMetrics() != null && !metrics.getProviderMetrics().isEmpty()) {
            csv.append("Provider Metrics\n");
            csv.append("Provider,Sent,Delivered,Failed,Delivery Rate (%),Failure Rate (%),Avg Delivery Time (s)\n");
            metrics.getProviderMetrics().forEach((provider, providerMetrics) -> {
                csv.append(provider).append(",")
                   .append(providerMetrics.getSent()).append(",")
                   .append(providerMetrics.getDelivered()).append(",")
                   .append(providerMetrics.getFailed()).append(",")
                   .append(String.format("%.2f", providerMetrics.getDeliveryRate())).append(",")
                   .append(String.format("%.2f", providerMetrics.getFailureRate())).append(",")
                   .append(String.format("%.2f", providerMetrics.getAverageDeliveryTimeSeconds())).append("\n");
            });
            csv.append("\n");
        }
        
        if (metrics.getFailureReasons() != null && !metrics.getFailureReasons().isEmpty()) {
            csv.append("Failure Reasons\n");
            csv.append("Reason,Count\n");
            metrics.getFailureReasons().forEach((reason, count) -> {
                csv.append(reason).append(",").append(count).append("\n");
            });
        }
        
        return csv.toString();
    }
    
    /**
     * Convert analytics metrics to JSON format
     */
    private String convertToJson(AnalyticsMetrics metrics) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"totalSent\": ").append(metrics.getTotalSent()).append(",\n");
        json.append("  \"totalDelivered\": ").append(metrics.getTotalDelivered()).append(",\n");
        json.append("  \"totalFailed\": ").append(metrics.getTotalFailed()).append(",\n");
        json.append("  \"totalRead\": ").append(metrics.getTotalRead()).append(",\n");
        json.append("  \"deliveryRate\": ").append(String.format("%.2f", metrics.getDeliveryRate())).append(",\n");
        json.append("  \"failureRate\": ").append(String.format("%.2f", metrics.getFailureRate())).append(",\n");
        json.append("  \"readRate\": ").append(String.format("%.2f", metrics.getReadRate())).append(",\n");
        json.append("  \"averageDeliveryTimeSeconds\": ").append(String.format("%.2f", metrics.getAverageDeliveryTimeSeconds())).append(",\n");
        
        if (metrics.getChannelMetrics() != null) {
            json.append("  \"channelMetrics\": {\n");
            String channelJson = metrics.getChannelMetrics().entrySet().stream()
                .map(e -> String.format("    \"%s\": {\"sent\": %d, \"delivered\": %d, \"failed\": %d, \"read\": %d, \"deliveryRate\": %.2f, \"failureRate\": %.2f}",
                    e.getKey(), e.getValue().getSent(), e.getValue().getDelivered(), 
                    e.getValue().getFailed(), e.getValue().getRead(),
                    e.getValue().getDeliveryRate(), e.getValue().getFailureRate()))
                .collect(Collectors.joining(",\n"));
            json.append(channelJson).append("\n  },\n");
        }
        
        if (metrics.getProviderMetrics() != null) {
            json.append("  \"providerMetrics\": {\n");
            String providerJson = metrics.getProviderMetrics().entrySet().stream()
                .map(e -> String.format("    \"%s\": {\"sent\": %d, \"delivered\": %d, \"failed\": %d, \"deliveryRate\": %.2f, \"failureRate\": %.2f}",
                    e.getKey(), e.getValue().getSent(), e.getValue().getDelivered(), 
                    e.getValue().getFailed(), e.getValue().getDeliveryRate(), e.getValue().getFailureRate()))
                .collect(Collectors.joining(",\n"));
            json.append(providerJson).append("\n  },\n");
        }
        

        if (metrics.getFailureReasons() != null) {
            json.append("  \"failureReasons\": {\n");
            String failureJson = metrics.getFailureReasons().entrySet().stream()
                .map(e -> String.format("    \"%s\": %d", e.getKey(), e.getValue()))
                .collect(Collectors.joining(",\n"));
            json.append(failureJson).append("\n  }\n");
        }
        
        json.append("}\n");
        return json.toString();
    }
}
