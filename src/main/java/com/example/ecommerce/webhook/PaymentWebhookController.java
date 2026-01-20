package com.example.ecommerce.webhook;

import com.example.ecommerce.dto.PaymentWebhookRequest;
import com.example.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/payment")
    public ResponseEntity<Map<String, String>> handlePaymentWebhook(@RequestBody PaymentWebhookRequest request) {
        log.info("POST /api/webhooks/payment - Received payment webhook for order: {}", request.getOrderId());
        log.info("Payment status: {}, Payment ID: {}", request.getStatus(), request.getPaymentId());

        paymentService.processWebhook(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Webhook processed successfully");
        return ResponseEntity.ok(response);
    }
}
