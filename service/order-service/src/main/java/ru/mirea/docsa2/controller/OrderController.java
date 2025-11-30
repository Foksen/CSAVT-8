package ru.mirea.docsa2.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.docsa2.dto.CreateOrderRequest;
import ru.mirea.docsa2.dto.OrderResponse;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.OrderCreatedEvent;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.model.Order;
import ru.mirea.docsa2.producer.OrderEventProducer;
import ru.mirea.docsa2.repository.OrderRepository;
import ru.mirea.docsa2.service.ValidationService;
import ru.mirea.docsa2.util.AuthenticationUtil;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ValidationService validationService;
    private final OrderEventProducer orderEventProducer;

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> getOrdersByCustomerId(@PathVariable Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @GetMapping("/self")
    public ResponseEntity<?> getSelfOrders(Authentication authentication) {
        Long userId = AuthenticationUtil.extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.badRequest().body("User ID not found in token");
        }

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Self orders endpoint requires customer lookup - implement via event sourcing");
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        try {
            String username = AuthenticationUtil.extractUsername(authentication);
            log.info("User '{}' is creating order for product {}", username, request.productId());

            CompletableFuture<ProductValidationResponse> productValidation = 
                validationService.validateProduct(request.productId(), request.quantity());
            
            CompletableFuture<CustomerValidationResponse> customerValidation = 
                validationService.validateCustomer(request.customerId());

            CompletableFuture.allOf(productValidation, customerValidation).join();

            ProductValidationResponse productResponse = productValidation.get();
            CustomerValidationResponse customerResponse = customerValidation.get();

            if (productResponse == null || !productResponse.valid()) {
                String error = productResponse != null ? productResponse.errorMessage() : "Product validation timeout";
                return ResponseEntity.badRequest().body(error);
            }

            if (customerResponse == null || !customerResponse.valid()) {
                String error = customerResponse != null ? customerResponse.errorMessage() : "Customer validation timeout";
                return ResponseEntity.badRequest().body(error);
            }

            BigDecimal totalPrice = productResponse.price().multiply(BigDecimal.valueOf(request.quantity()));
            
            Order order = new Order();
            order.setCustomerId(request.customerId());
            order.setProductId(request.productId());
            order.setQuantity(request.quantity());
            order.setTotalPrice(totalPrice);
            order.setStatus(Order.OrderStatus.CONFIRMED);

            Order saved = orderRepository.save(order);
            log.info("Order created successfully: {}", saved.getId());

            OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getTotalPrice(),
                LocalDateTime.now()
            );
            orderEventProducer.sendOrderCreatedEvent(event);

            return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(saved));
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating order: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestParam Order.OrderStatus status) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    Order updated = orderRepository.save(order);
                    return ResponseEntity.ok(OrderResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (!orderRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        orderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

