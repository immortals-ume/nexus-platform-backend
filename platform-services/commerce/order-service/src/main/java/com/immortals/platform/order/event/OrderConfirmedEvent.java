package com.immortals.platform.order.event;

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
    private String paymentMethod;
    private List<Object> products;
    private Instant confirmedAt;
}