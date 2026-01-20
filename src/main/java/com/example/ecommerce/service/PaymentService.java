package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentRequest;
import com.example.ecommerce.dto.PaymentWebhookRequest;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final RestTemplate restTemplate;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Transactional
    public Payment createPayment(PaymentRequest request) {
        log.info("Creating payment for order: {}", request.getOrderId());

        // Validate order exists and status is CREATED
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (!"CREATED".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not in CREATED status. Current status: " + order.getStatus());
        }

        // Check if payment already exists for this order
        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new IllegalArgumentException("Payment already exists for this order");
        }

        // Create payment record
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status("PENDING")
                .paymentId("pending") // Will be updated by payment service
                .createdAt(Instant.now())
                .build();
        payment = paymentRepository.save(payment);
        log.info("Payment created with id: {}", payment.getId());

        // Call mock payment service
        try {
            Map<String, Object> paymentPayload = new HashMap<>();
            paymentPayload.put("orderId", request.getOrderId());
            paymentPayload.put("amount", request.getAmount());
            paymentPayload.put("paymentId", payment.getId());

            String url = paymentServiceUrl + "/payments/create";
            log.info("Calling payment service at: {}", url);

            restTemplate.postForObject(url, paymentPayload, String.class);
            log.info("Payment request sent to payment service");
        } catch (Exception e) {
            log.error("Failed to call payment service: {}", e.getMessage());
            // Don't fail the payment creation, webhook might still work
        }

        return payment;
    }

    @Transactional
    public void processWebhook(PaymentWebhookRequest webhookRequest) {
        log.info("Processing payment webhook for order: {}", webhookRequest.getOrderId());

        // Find payment by order ID
        Payment payment = paymentRepository.findByOrderId(webhookRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + webhookRequest.getOrderId()));

        // Update payment status
        payment.setStatus(webhookRequest.getStatus());
        payment.setPaymentId(webhookRequest.getPaymentId());
        paymentRepository.save(payment);
        log.info("Payment status updated to: {}", webhookRequest.getStatus());

        // Update order status based on payment status
        String orderStatus = "SUCCESS".equals(webhookRequest.getStatus()) ? "PAID" : "FAILED";
        orderService.updateOrderStatus(webhookRequest.getOrderId(), orderStatus);
        log.info("Order status updated to: {}", orderStatus);
    }
}
