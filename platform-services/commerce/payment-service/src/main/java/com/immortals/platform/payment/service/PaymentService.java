package com.immortals.platform.payment.service;

import com.immortals.platform.payment.dto.request.PaymentRequest;
import com.immortals.platform.payment.mapper.PaymentMapper;
import com.immortals.platform.payment.messaging.NotificationProducer;
import com.immortals.platform.payment.dto.response.PaymentNotificationRequest;
import com.immortals.platform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository repository;
  private final PaymentMapper mapper;
  private final NotificationProducer notificationProducer;

  public Integer createPayment(PaymentRequest request) {
    var payment = this.repository.save(this.mapper.toPayment(request));

    this.notificationProducer.sendNotification(
            new PaymentNotificationRequest(
                    request.orderReference(),
                    request.amount(),
                    request.paymentMethod(),
                    request.customer().firstname(),
                    request.customer().lastname(),
                    request.customer().email()
            )
    );
    return payment.getId();
  }
}
