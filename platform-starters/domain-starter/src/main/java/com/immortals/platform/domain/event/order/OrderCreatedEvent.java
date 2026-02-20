package com.immortals.platform.domain.event.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event published when an order is created
 */
@Data
@Builder
public class OrderCreatedEvent {
    private Integer orderId;
    private String reference;
    private String customerId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String paymentMethod; // Using String to avoid enum dependency issues
    private String status; // Using String to avoid enum dependency issues
    private Instant createdAt;
}