package com.immortals.platform.order.dto.request;

import com.immortals.platform.order.dto.response.CustomerResponse;
import com.immortals.platform.domain.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
    BigDecimal amount,
    PaymentMethod paymentMethod,
    Integer orderId,
    String orderReference,
    CustomerResponse customer
) {
}
