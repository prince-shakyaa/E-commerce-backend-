package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;

    @Transactional
    public CartItem addToCart(AddToCartRequest request) {
        log.info("Adding to cart: userId={}, productId={}, quantity={}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        // Validate product exists
        Product product = productService.getProductById(request.getProductId());

        // Check stock availability
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock available. Available: " + product.getStock());
        }

        // Check if item already in cart
        Optional<CartItem> existingItem = cartRepository.findByUserIdAndProductId(
                request.getUserId(), request.getProductId());

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            if (product.getStock() < newQuantity) {
                throw new IllegalArgumentException("Insufficient stock available. Available: " + product.getStock());
            }

            cartItem.setQuantity(newQuantity);
            log.info("Updated cart item quantity to: {}", newQuantity);
            return cartRepository.save(cartItem);
        } else {
            // Add new item
            CartItem cartItem = CartItem.builder()
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .build();
            log.info("Added new cart item");
            return cartRepository.save(cartItem);
        }
    }

    public List<CartItemResponse> getCartItems(String userId) {
        log.info("Fetching cart items for user: {}", userId);
        List<CartItem> cartItems = cartRepository.findByUserId(userId);

        return cartItems.stream()
                .map(item -> {
                    Product product = productService.getProductById(item.getProductId());
                    return CartItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .product(product)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void clearCart(String userId) {
        log.info("Clearing cart for user: {}", userId);
        cartRepository.deleteByUserId(userId);
    }
}
