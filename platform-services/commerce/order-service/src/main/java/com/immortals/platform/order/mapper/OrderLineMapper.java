package com.immortals.platform.order.mapper;

import com.immortals.platform.domain.entity.Order;
import com.immortals.platform.domain.entity.OrderLine;
import com.immortals.platform.order.dto.request.OrderLineRequest;
import com.immortals.platform.order.dto.response.OrderLineResponse;
import org.springframework.stereotype.Service;

@Service
public class OrderLineMapper {
    public OrderLine toOrderLine(OrderLineRequest request) {
        return OrderLine.builder()
                .id(request.orderId())
                .productId(request.productId())
                .order(
                        Order.builder()
                                .id(request.orderId())
                                .build()
                )
                .quantity(request.quantity())
                .build();
    }

    public OrderLineResponse toOrderLineResponse(OrderLine orderLine) {
        return new OrderLineResponse(
                orderLine.getId(),
                orderLine.getQuantity()
        );
    }
}
