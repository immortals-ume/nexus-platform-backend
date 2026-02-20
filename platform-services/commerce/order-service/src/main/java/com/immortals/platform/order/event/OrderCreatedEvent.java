package com.immortals.platform.order.event;

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
    private String paymentMethod;
    private String status;
    private Instant createdAt;
}