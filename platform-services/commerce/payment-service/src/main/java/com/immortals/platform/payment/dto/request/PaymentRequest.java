package com.immortals.platform.payment.dto.request;

import com.immortals.platform.domain.enums.PaymentMethod;
import com.immortals.platform.payment.dto.response.Customer;

import java.math.BigDecimal;

public record PaymentRequest(
    Integer id,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    Integer orderId,
    String orderReference,
    Customer customer
) {
}
