package com.immortals.notification.service.service.impl;

import com.immortals.notification.service.dto.AnalyticsFilter;
import com.immortals.notification.service.dto.AnalyticsMetrics;
import com.immortals.notification.service.repository.NotificationLogRepository;
import com.immortals.notification.service.service.NotificationAnalyticsService;
import com.immortals.platform.domain.notifications.domain.model.Notification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationAnalyticsService
 * Provides comprehensive analytics with aggregation queries and filtering support
 * Requirements: 10.1, 10.2, 10.3, 10.4
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationAnalyticsServiceImpl implements NotificationAnalyticsService {
    
    private final NotificationLogRepository notificationLogRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @Transactional
    public void recordSent(Notification notification) {
        log.debug("Recording sent notification: {}", notification.getId());
        // Implementation handled by notification processing
    }
    
    @Override
    @Transactional
    public void recordDelivered(String notificationId) {
        log.debug("Recording delivered notification: {}", notificationId);
        // Implementation handled by webhook processing
    }
    
    @Override
    @Transactional
    public void recordOpened(String notificationId) {
        log.debug("Recording opened notification: {}", notificationId);
        // Implementation handled by webhook processing
    }
    
    @Override
    @Transactional
    public void recordClicked(String notificationId) {
        log.debug("Recording clicked notification: {}", notificationId);
        // Implementation handled by webhook processing
    }
    
    @Override
    @Transactional
    public void recordBounced(String notificationId, String reason) {
        log.debug("Recording bounced notification: {} with reason: {}", notificationId, reason);
        // Implementation handled by webhook processing
    }
    
    @Override
    public double getDeliveryRate(LocalDateTime start, LocalDateTime end) {
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(start)
            .endDate(end)
            .build();
        AnalyticsMetrics metrics = getAnalytics(filter);
        return metrics.getDeliveryRate() != null ? metrics.getDeliveryRate() : 0.0;
    }
    
    @Override
    public double getOpenRate(LocalDateTime start, LocalDateTime end) {
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(start)
            .endDate(end)
            .build();
        AnalyticsMetrics metrics = getAnalytics(filter);
        return metrics.getReadRate() != null ? metrics.getReadRate() : 0.0;
    }
    
    @Override
    public double getClickRate(LocalDateTime start, LocalDateTime end) {
        // Click tracking would require additional fields in the schema
        // For now, return 0.0 as placeholder
        return 0.0;
    }
    
    @Override
    public double getBounceRate(LocalDateTime start, LocalDateTime end) {
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .startDate(start)
            .endDate(end)
            .build();
        AnalyticsMetrics metrics = getAnalytics(filter);
        return metrics.getFailureRate() != null ? metrics.getFailureRate() : 0.0;
    }
    
    @Override
    public Map<String, Object> getProviderMetrics(String providerId) {
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .providerId(providerId)
            .build();
        Map<String, AnalyticsMetrics.ProviderMetrics> providerMetrics = getMetricsByProvider(filter);
        return providerMetrics.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue()));
    }
    
    @Override
    public Map<String, Object> getTypeMetrics(Notification.NotificationType type) {
        AnalyticsFilter filter = AnalyticsFilter.builder()
            .channel(type)
            .build();
        Map<String, AnalyticsMetrics.ChannelMetrics> channelMetrics = getMetricsByChannel(filter);
        return channelMetrics.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue()));
    }
    
    @Override
    public AnalyticsMetrics getAnalytics(AnalyticsFilter filter) {
        log.info("Getting analytics with filter: {}", filter);
        
        // Build base query with filters
        String whereClause = buildWhereClause(filter);
        
        // Get overall metrics
        String metricsQuery = "SELECT " +
            "COUNT(*) as total_sent, " +
            "SUM(CASE WHEN delivery_status = 'DELIVERED' THEN 1 ELSE 0 END) as total_delivered, " +
            "SUM(CASE WHEN status = 'FAILED' OR delivery_status = 'FAILED' THEN 1 ELSE 0 END) as total_failed, " +
            "SUM(CASE WHEN delivery_status = 'READ' THEN 1 ELSE 0 END) as total_read " +
            "FROM notification_logs " + whereClause;
        
        Query query = entityManager.createNativeQuery(metricsQuery);
        setQueryParameters(query, filter);
        Object[] result = (Object[]) query.getSingleResult();
        
        Long totalSent = ((Number) result[0]).longValue();
        Long totalDelivered = ((Number) result[1]).longValue();
        Long totalFailed = ((Number) result[2]).longValue();
        Long totalRead = ((Number) result[3]).longValue();
        
        // Calculate rates
        Double deliveryRate = totalSent > 0 ? (totalDelivered.doubleValue() / totalSent) * 100 : 0.0;
        Double failureRate = totalSent > 0 ? (totalFailed.doubleValue() / totalSent) * 100 : 0.0;
        Double readRate = totalDelivered > 0 ? (totalRead.doubleValue() / totalDelivered) * 100 : 0.0;
        
        // Calculate average delivery time
        Double avgDeliveryTime = calculateAverageDeliveryTime(filter);
        
        return AnalyticsMetrics.builder()
            .totalSent(totalSent)
            .totalDelivered(totalDelivered)
            .totalFailed(totalFailed)
            .totalRead(totalRead)
            .deliveryRate(deliveryRate)
            .failureRate(failureRate)
            .readRate(readRate)
            .averageDeliveryTimeSeconds(avgDeliveryTime)
            .channelMetrics(getMetricsByChannel(filter))
            .providerMetrics(getMetricsByProvider(filter))
            .countryMetrics(getMetricsByCountry(filter))
            .failureReasons(getFailureReasons(filter))
            .build();
    }
    
    @Override
    public Map<String, AnalyticsMetrics.ChannelMetrics> getMetricsByChannel(AnalyticsFilter filter) {
        log.debug("Getting metrics by channel with filter: {}", filter);
        
        String whereClause = buildWhereClause(filter);
        String query = "SELECT " +
            "notification_type, " +
            "COUNT(*) as sent, " +
            "SUM(CASE WHEN delivery_status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered, " +
            "SUM(CASE WHEN status = 'FAILED' OR delivery_status = 'FAILED' THEN 1 ELSE 0 END) as failed, " +
            "SUM(CASE WHEN delivery_status = 'READ' THEN 1 ELSE 0 END) as read " +
            "FROM notification_logs " + whereClause +
            " GROUP BY notification_type";
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        setQueryParameters(nativeQuery, filter);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        Map<String, AnalyticsMetrics.ChannelMetrics> channelMetrics = new HashMap<>();
        for (Object[] row : results) {
            String channel = (String) row[0];
            Long sent = ((Number) row[1]).longValue();
            Long delivered = ((Number) row[2]).longValue();
            Long failed = ((Number) row[3]).longValue();
            Long read = ((Number) row[4]).longValue();
            
            Double deliveryRate = sent > 0 ? (delivered.doubleValue() / sent) * 100 : 0.0;
            Double failureRate = sent > 0 ? (failed.doubleValue() / sent) * 100 : 0.0;
            
            // Calculate average delivery time for this channel
            AnalyticsFilter channelFilter = AnalyticsFilter.builder()
                .startDate(filter.getStartDate())
                .endDate(filter.getEndDate())
                .channel(Notification.NotificationType.valueOf(channel))
                .providerId(filter.getProviderId())
                .countryCode(filter.getCountryCode())
                .build();
            Double avgDeliveryTime = calculateAverageDeliveryTime(channelFilter);
            
            channelMetrics.put(channel, AnalyticsMetrics.ChannelMetrics.builder()
                .channel(channel)
                .sent(sent)
                .delivered(delivered)
                .failed(failed)
                .read(read)
                .deliveryRate(deliveryRate)
                .failureRate(failureRate)
                .averageDeliveryTimeSeconds(avgDeliveryTime)
                .build());
        }
        
        return channelMetrics;
    }
    
    @Override
    public Map<String, AnalyticsMetrics.ProviderMetrics> getMetricsByProvider(AnalyticsFilter filter) {
        log.debug("Getting metrics by provider with filter: {}", filter);
        
        String whereClause = buildWhereClause(filter);
        String query = "SELECT " +
            "provider_id, " +
            "COUNT(*) as sent, " +
            "SUM(CASE WHEN delivery_status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered, " +
            "SUM(CASE WHEN status = 'FAILED' OR delivery_status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
            "FROM notification_logs " + whereClause +
            " AND provider_id IS NOT NULL " +
            " GROUP BY provider_id";
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        setQueryParameters(nativeQuery, filter);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        Map<String, AnalyticsMetrics.ProviderMetrics> providerMetrics = new HashMap<>();
        for (Object[] row : results) {
            String providerId = (String) row[0];
            Long sent = ((Number) row[1]).longValue();
            Long delivered = ((Number) row[2]).longValue();
            Long failed = ((Number) row[3]).longValue();
            
            Double deliveryRate = sent > 0 ? (delivered.doubleValue() / sent) * 100 : 0.0;
            Double failureRate = sent > 0 ? (failed.doubleValue() / sent) * 100 : 0.0;
            
            // Calculate average delivery time for this provider
            AnalyticsFilter providerFilter = AnalyticsFilter.builder()
                .startDate(filter.getStartDate())
                .endDate(filter.getEndDate())
                .channel(filter.getChannel())
                .providerId(providerId)
                .countryCode(filter.getCountryCode())
                .build();
            Double avgDeliveryTime = calculateAverageDeliveryTime(providerFilter);
            
            providerMetrics.put(providerId, AnalyticsMetrics.ProviderMetrics.builder()
                .providerId(providerId)
                .sent(sent)
                .delivered(delivered)
                .failed(failed)
                .deliveryRate(deliveryRate)
                .failureRate(failureRate)
                .averageDeliveryTimeSeconds(avgDeliveryTime)
                .build());
        }
        
        return providerMetrics;
    }
    
    @Override
    public Map<String, AnalyticsMetrics.CountryMetrics> getMetricsByCountry(AnalyticsFilter filter) {
        log.debug("Getting metrics by country with filter: {}", filter);
        
        String whereClause = buildWhereClause(filter);
        String query = "SELECT " +
            "country_code, " +
            "COUNT(*) as sent, " +
            "SUM(CASE WHEN delivery_status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered, " +
            "SUM(CASE WHEN status = 'FAILED' OR delivery_status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
            "FROM notification_logs " + whereClause +
            " AND country_code IS NOT NULL " +
            " GROUP BY country_code";
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        setQueryParameters(nativeQuery, filter);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        Map<String, AnalyticsMetrics.CountryMetrics> countryMetrics = new HashMap<>();
        for (Object[] row : results) {
            String countryCode = (String) row[0];
            Long sent = ((Number) row[1]).longValue();
            Long delivered = ((Number) row[2]).longValue();
            Long failed = ((Number) row[3]).longValue();
            
            Double deliveryRate = sent > 0 ? (delivered.doubleValue() / sent) * 100 : 0.0;
            
            countryMetrics.put(countryCode, AnalyticsMetrics.CountryMetrics.builder()
                .countryCode(countryCode)
                .sent(sent)
                .delivered(delivered)
                .failed(failed)
                .deliveryRate(deliveryRate)
                .build());
        }
        
        return countryMetrics;
    }
    
    @Override
    public Map<String, Long> getFailureReasons(AnalyticsFilter filter) {
        log.debug("Getting failure reasons with filter: {}", filter);
        
        String whereClause = buildWhereClause(filter);
        String query = "SELECT " +
            "CASE " +
            "  WHEN error_message LIKE '%rate limit%' OR error_message LIKE '%429%' THEN 'RATE_LIMIT_EXCEEDED' " +
            "  WHEN error_message LIKE '%invalid%recipient%' OR error_message LIKE '%invalid%phone%' OR error_message LIKE '%invalid%email%' THEN 'INVALID_RECIPIENT' " +
            "  WHEN error_message LIKE '%authentication%' OR error_message LIKE '%401%' OR error_message LIKE '%403%' THEN 'AUTHENTICATION_ERROR' " +
            "  WHEN error_message LIKE '%timeout%' OR error_message LIKE '%timed out%' THEN 'TIMEOUT' " +
            "  WHEN error_message LIKE '%network%' OR error_message LIKE '%connection%' THEN 'NETWORK_ERROR' " +
            "  WHEN error_message LIKE '%provider%unavailable%' OR error_message LIKE '%503%' THEN 'PROVIDER_UNAVAILABLE' " +
            "  WHEN error_message IS NOT NULL THEN 'PROVIDER_ERROR' " +
            "  ELSE 'UNKNOWN' " +
            "END as failure_reason, " +
            "COUNT(*) as count " +
            "FROM notification_logs " + whereClause +
            " AND (status = 'FAILED' OR delivery_status = 'FAILED') " +
            " GROUP BY failure_reason";
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        setQueryParameters(nativeQuery, filter);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        Map<String, Long> failureReasons = new HashMap<>();
        for (Object[] row : results) {
            String reason = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            failureReasons.put(reason, count);
        }
        
        return failureReasons;
    }
    
    @Override
    public Map<String, Double> getAverageDeliveryTimeByChannel(AnalyticsFilter filter) {
        log.debug("Getting average delivery time by channel with filter: {}", filter);
        
        String whereClause = buildWhereClause(filter);
        String query = "SELECT " +
            "notification_type, " +
            "AVG(EXTRACT(EPOCH FROM (delivered_at - processed_at))) as avg_delivery_time " +
            "FROM notification_logs " + whereClause +
            " AND delivered_at IS NOT NULL AND processed_at IS NOT NULL " +
            " GROUP BY notification_type";
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        setQueryParameters(nativeQuery, filter);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        Map<String, Double> avgTimes = new HashMap<>();
        for (Object[] row : results) {
            String channel = (String) row[0];
            Double avgTime = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            avgTimes.put(channel, avgTime);
        }
        
        return avgTimes;
    }
    
    @Override
    public Map<String, Double> getAverageDeliveryTimeByProvider(AnalyticsFilter filter) {
        log.debug("Getting average delivery time by provider with filter: {}", filter);
        
        String whereClause = buildWhereClause(filter);
        String query = "SELECT " +
            "provider_id, " +
            "AVG(EXTRACT(EPOCH FROM (delivered_at - processed_at))) as avg_delivery_time " +
            "FROM notification_logs " + whereClause +
            " AND provider_id IS NOT NULL " +
            " AND delivered_at IS NOT NULL AND processed_at IS NOT NULL " +
            " GROUP BY provider_id";
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        setQueryParameters(nativeQuery, filter);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        Map<String, Double> avgTimes = new HashMap<>();
        for (Object[] row : results) {
            String providerId = (String) row[0];
            Double avgTime = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            avgTimes.put(providerId, avgTime);
        }
        
        return avgTimes;
    }
    
    /**
     * Build WHERE clause based on filter criteria
     */
    private String buildWhereClause(AnalyticsFilter filter) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        
        if (filter.hasDateRange()) {
            where.append(" AND created_at BETWEEN :startDate AND :endDate");
        }
        
        if (filter.hasChannel()) {
            where.append(" AND notification_type = :channel");
        }
        
        if (filter.hasProvider()) {
            where.append(" AND provider_id = :providerId");
        }
        
        if (filter.hasCountry()) {
            where.append(" AND country_code = :countryCode");
        }
        
        return where.toString();
    }
    
    /**
     * Set query parameters based on filter
     */
    private void setQueryParameters(Query query, AnalyticsFilter filter) {
        if (filter.hasDateRange()) {
            query.setParameter("startDate", filter.getStartDate());
            query.setParameter("endDate", filter.getEndDate());
        }
        
        if (filter.hasChannel()) {
            query.setParameter("channel", filter.getChannel().name());
        }
        
        if (filter.hasProvider()) {
            query.setParameter("providerId", filter.getProviderId());
        }
        
        if (filter.hasCountry()) {
            query.setParameter("countryCode", filter.getCountryCode());
        }
    }
    
    /**
     * Calculate average delivery time in seconds
     */
    private Double calculateAverageDeliveryTime(AnalyticsFilter filter) {
        String whereClause = buildWhereClause(filter);
        String query = "SELECT AVG(EXTRACT(EPOCH FROM (delivered_at - processed_at))) " +
            "FROM notification_logs " + whereClause +
            " AND delivered_at IS NOT NULL AND processed_at IS NOT NULL";
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        setQueryParameters(nativeQuery, filter);
        
        Object result = nativeQuery.getSingleResult();
        return result != null ? ((Number) result).doubleValue() : 0.0;
    }
}
