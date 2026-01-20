package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class MockPaymentController {

    private final RestTemplate restTemplate;

    @Value("${ecommerce.webhook.url}")
    private String webhookUrl;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@RequestBody PaymentRequest request) {
        log.info("Received payment request for order: {}, amount: {}", request.getOrderId(), request.getAmount());

        // Generate mock payment ID
        String mockPaymentId = "pay_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Generated mock payment ID: {}", mockPaymentId);

        // Process payment asynchronously (simulate 3 second delay)
        processPaymentAsync(request.getOrderId(), mockPaymentId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Payment processing started");
        response.put("paymentId", mockPaymentId);
        response.put("orderId", request.getOrderId());
        response.put("status", "PROCESSING");

        return ResponseEntity.ok(response);
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> processPaymentAsync(String orderId, String paymentId) {
        try {
            log.info("Processing payment... (waiting 3 seconds)");
            Thread.sleep(3000); // Simulate payment processing

            // 90% success rate for demo purposes
            boolean success = Math.random() < 0.9;
            String status = success ? "SUCCESS" : "FAILED";
            String message = success ? "Payment completed successfully" : "Payment failed";

            log.info("Payment processing completed: {}", status);

            // Send webhook to e-commerce API
            PaymentWebhookRequest webhookRequest = PaymentWebhookRequest.builder()
                    .orderId(orderId)
                    .paymentId(paymentId)
                    .status(status)
                    .message(message)
                    .build();

            log.info("Sending webhook to: {}", webhookUrl);
            restTemplate.postForObject(webhookUrl, webhookRequest, String.class);
            log.info("Webhook sent successfully");

        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }
}
