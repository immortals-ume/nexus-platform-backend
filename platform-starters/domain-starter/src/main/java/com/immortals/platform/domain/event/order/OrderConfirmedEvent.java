package com.immortals.platform.domain.event.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Domain event published when an order is confirmed
 */
@Data
@Builder
public class OrderConfirmedEvent {
    private String reference;
    private String customerId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String paymentMethod; // Using String to avoid enum dependency issues
    private List<Object> products; // Using Object to avoid circular dependencies
    private Instant confirmedAt;
}