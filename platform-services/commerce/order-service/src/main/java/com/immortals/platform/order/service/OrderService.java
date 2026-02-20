package com.immortals.platform.order.service;

import com.immortals.platform.order.dto.request.OrderLineRequest;
import com.immortals.platform.order.dto.request.OrderRequest;
import com.immortals.platform.order.dto.request.PaymentRequest;
import com.immortals.platform.order.dto.request.PurchaseRequest;
import com.immortals.platform.order.dto.response.OrderConfirmation;
import com.immortals.platform.order.dto.response.OrderResponse;
import com.immortals.platform.order.client.CustomerClient;
import com.immortals.platform.common.exception.BusinessException;
import com.immortals.platform.order.client.PaymentClient;
import com.immortals.platform.order.client.ProductClient;
import com.immortals.platform.order.mapper.OrderMapper;
import com.immortals.platform.order.repository.OrderRepository;
import com.immortals.platform.domain.shared.event.DomainEvent;
import com.immortals.platform.messaging.publisher.EventPublisher;
import com.immortals.platform.order.config.KafkaOrderTopicConfig;
import com.immortals.platform.order.event.OrderCreatedEvent;
import com.immortals.platform.order.event.OrderConfirmedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final CustomerClient customerClient;
    private final PaymentClient paymentClient;
    private final ProductClient productClient;
    private final OrderLineService orderLineService;
    private final EventPublisher eventPublisher;
    private final KafkaOrderTopicConfig kafkaOrderTopicConfig;

    @Transactional
    public Integer createOrder(OrderRequest request) {
        log.info("Creating order with reference: {}", request.reference());

        try {
            var customer = this.customerClient.findCustomerById(request.customerId())
                    .orElseThrow(() -> new BusinessException("Cannot create order:: No customer exists with the provided ID"));

            var purchasedProducts = productClient.purchaseProducts(request.products());

            var order = this.repository.save(mapper.toOrder(request));

            for (PurchaseRequest purchaseRequest : request.products()) {
                orderLineService.saveOrderLine(
                        new OrderLineRequest(
                                null,
                                order.getId(),
                                purchaseRequest.productId(),
                                purchaseRequest.quantity()
                        )
                );
            }

            // Publish OrderCreatedEvent
            publishOrderCreatedEvent(order, customer, purchasedProducts);

            var paymentRequest = new PaymentRequest(
                    request.amount(),
                    request.paymentMethod(),
                    order.getId(),
                    order.getReference(),
                    customer
            );
            paymentClient.requestOrderPayment(paymentRequest);

            // Publish OrderConfirmedEvent after payment request
            publishOrderConfirmedEvent(request, customer, purchasedProducts);

            log.info("Order created successfully with ID: {}", order.getId());
            return order.getId();

        } catch (BusinessException e) {
            log.error("Business error creating order: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating order with reference: {}", request.reference(), e);
            throw new BusinessException("Failed to create order: " + e.getMessage());
        }
    }

    public List<OrderResponse> findAllOrders() {
        return this.repository.findAll()
                .stream()
                .map(this.mapper::fromOrder)
                .collect(Collectors.toList());
    }

    public OrderResponse findById(Integer id) {
        return this.repository.findById(id)
                .map(this.mapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException(String.format("No order found with the provided ID: %d", id)));
    }

    /**
     * Publish OrderCreatedEvent to Kafka
     */
    private void publishOrderCreatedEvent(Object order, Object customer, Object purchasedProducts) {
        try {
            OrderCreatedEvent eventPayload = OrderCreatedEvent.builder()
                    .orderId(((com.immortals.platform.domain.entity.Order) order).getId())
                    .reference(((com.immortals.platform.domain.entity.Order) order).getReference())
                    .customerId(((com.immortals.platform.order.dto.response.CustomerResponse) customer).id())
                    .customerEmail(((com.immortals.platform.order.dto.response.CustomerResponse) customer).email())
                    .totalAmount(((com.immortals.platform.domain.entity.Order) order).getTotalAmount())
                    .paymentMethod(((com.immortals.platform.domain.entity.Order) order).getPaymentMethod().toString())
                    .status("CREATED") // Default status since we don't have access to the status field
                    .createdAt(java.time.Instant.now())
                    .build();

            DomainEvent<OrderCreatedEvent> domainEvent = DomainEvent.<OrderCreatedEvent>builder()
                    .eventType("OrderCreated")
                    .aggregateId(((com.immortals.platform.domain.entity.Order) order).getId().toString())
                    .aggregateType("Order")
                    .payload(eventPayload)
                    .correlationId(getCorrelationId())
                    .build();

            eventPublisher.publish("order-created-topic", domainEvent);
            
            log.info("Published OrderCreatedEvent for order ID: {} with correlation ID: {}", 
                ((com.immortals.platform.domain.entity.Order) order).getId(), domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent for order ID: {}", 
                ((com.immortals.platform.domain.entity.Order) order).getId(), e);
        }
    }

    /**
     * Publish OrderConfirmedEvent to Kafka
     */
    @SuppressWarnings("unchecked")
    private void publishOrderConfirmedEvent(OrderRequest request, Object customer, Object purchasedProducts) {
        try {
            OrderConfirmedEvent eventPayload = OrderConfirmedEvent.builder()
                    .reference(request.reference())
                    .customerId(request.customerId())
                    .customerEmail(((com.immortals.platform.order.dto.response.CustomerResponse) customer).email())
                    .totalAmount(request.amount())
                    .paymentMethod(request.paymentMethod().toString())
                    .products((List<Object>) purchasedProducts) // Cast to List<Object>
                    .confirmedAt(java.time.Instant.now())
                    .build();

            DomainEvent<OrderConfirmedEvent> domainEvent = DomainEvent.<OrderConfirmedEvent>builder()
                    .eventType("OrderConfirmed")
                    .aggregateId(request.reference())
                    .aggregateType("Order")
                    .payload(eventPayload)
                    .correlationId(getCorrelationId())
                    .build();

            eventPublisher.publish("order-confirmed-topic", domainEvent);
            
            log.info("Published OrderConfirmedEvent for order reference: {} with correlation ID: {}", 
                request.reference(), domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish OrderConfirmedEvent for order reference: {}", request.reference(), e);
        }
    }

    /**
     * Get correlation ID from MDC or generate new one
     */
    private String getCorrelationId() {
        try {
            String correlationId = org.slf4j.MDC.get("correlationId");
            if (correlationId != null && !correlationId.isBlank()) {
                return correlationId;
            }
        } catch (Exception e) {
            log.debug("Could not retrieve correlation ID from MDC", e);
        }
        return UUID.randomUUID().toString();
    }
}
