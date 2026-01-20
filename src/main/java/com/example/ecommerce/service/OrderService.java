package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.*;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final ProductService productService;

    @Transactional
    public OrderResponse createOrder(String userId) {
        log.info("Creating order for user: {}", userId);

        // Get cart items
        List<CartItem> cartItems = cartRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Validate stock and calculate total
        double totalAmount = 0.0;
        for (CartItem cartItem : cartItems) {
            Product product = productService.getProductById(cartItem.getProductId());

            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            totalAmount += product.getPrice() * cartItem.getQuantity();
        }

        // Create order
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .status("CREATED")
                .createdAt(Instant.now())
                .build();
        order = orderRepository.save(order);
        log.info("Order created with id: {}", order.getId());

        // Create order items and update stock
        final String orderId = order.getId();
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    Product product = productService.getProductById(cartItem.getProductId());

                    // Update stock
                    productService.updateStock(cartItem.getProductId(), cartItem.getQuantity());

                    // Create order item with snapshot price
                    return OrderItem.builder()
                            .orderId(orderId)
                            .productId(cartItem.getProductId())
                            .quantity(cartItem.getQuantity())
                            .price(product.getPrice())
                            .build();
                })
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);
        log.info("Created {} order items", orderItems.size());

        // Clear cart
        cartRepository.deleteByUserId(userId);
        log.info("Cart cleared for user: {}", userId);

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(orderItems)
                .build();
    }

    public OrderResponse getOrderById(String orderId) {
        log.info("Fetching order with id: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(orderItems)
                .payment(payment)
                .build();
    }

    @Transactional
    public void updateOrderStatus(String orderId, String status) {
        log.info("Updating order {} status to: {}", orderId, status);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setStatus(status);
        orderRepository.save(order);
    }

    public List<Order> getUserOrders(String userId) {
        log.info("Fetching orders for user: {}", userId);
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        log.info("Cancelling order: {}", orderId);

        // Get order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Validate order status - only CREATED orders can be cancelled
        if (!"CREATED".equals(order.getStatus())) {
            throw new IllegalArgumentException("Cannot cancel order with status: " + order.getStatus()
                    + ". Only CREATED orders can be cancelled.");
        }

        // Get order items to restore stock
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // Restore stock for each item
        for (OrderItem item : orderItems) {
            productService.restoreStock(item.getProductId(), item.getQuantity());
        }

        // Update order status to CANCELLED
        order.setStatus("CANCELLED");
        order = orderRepository.save(order);

        log.info("Order {} cancelled successfully. Stock restored.", orderId);

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(orderItems)
                .build();
    }
}
