package ru.mirea.docsa2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.mirea.docsa2.config.KafkaConfig;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.event.ValidateCustomerRequest;
import ru.mirea.docsa2.event.ValidateProductRequest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<ProductValidationResponse>> productValidationFutures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<CustomerValidationResponse>> customerValidationFutures = new ConcurrentHashMap<>();

    public CompletableFuture<ProductValidationResponse> validateProduct(Long productId, Integer quantity) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<ProductValidationResponse> future = new CompletableFuture<>();
        
        productValidationFutures.put(correlationId, future);
        
        ValidateProductRequest request = new ValidateProductRequest(correlationId, productId, quantity);
        log.info("Sending product validation request: {}", request);
        kafkaTemplate.send(KafkaConfig.VALIDATE_PRODUCT_REQUEST_TOPIC, correlationId, request);
        
        future.orTimeout(5, TimeUnit.SECONDS).exceptionally(ex -> {
            productValidationFutures.remove(correlationId);
            log.error("Product validation timeout for correlationId: {}", correlationId);
            return null;
        });
        
        return future;
    }

    public CompletableFuture<CustomerValidationResponse> validateCustomer(Long customerId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<CustomerValidationResponse> future = new CompletableFuture<>();
        
        customerValidationFutures.put(correlationId, future);
        
        ValidateCustomerRequest request = new ValidateCustomerRequest(correlationId, customerId);
        log.info("Sending customer validation request: {}", request);
        kafkaTemplate.send(KafkaConfig.VALIDATE_CUSTOMER_REQUEST_TOPIC, correlationId, request);
        
        future.orTimeout(5, TimeUnit.SECONDS).exceptionally(ex -> {
            customerValidationFutures.remove(correlationId);
            log.error("Customer validation timeout for correlationId: {}", correlationId);
            return null;
        });
        
        return future;
    }

    public void handleProductValidationResponse(ProductValidationResponse response) {
        log.info("Received product validation response: {}", response);
        CompletableFuture<ProductValidationResponse> future = productValidationFutures.remove(response.correlationId());
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("No pending future found for product validation correlationId: {}", response.correlationId());
        }
    }

    public void handleCustomerValidationResponse(CustomerValidationResponse response) {
        log.info("Received customer validation response: {}", response);
        CompletableFuture<CustomerValidationResponse> future = customerValidationFutures.remove(response.correlationId());
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("No pending future found for customer validation correlationId: {}", response.correlationId());
        }
    }
}

