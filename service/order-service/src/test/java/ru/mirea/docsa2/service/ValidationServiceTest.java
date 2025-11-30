package ru.mirea.docsa2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.mirea.docsa2.config.KafkaConfig;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.event.ValidateCustomerRequest;
import ru.mirea.docsa2.event.ValidateProductRequest;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ValidationService validationService;

    @Captor
    private ArgumentCaptor<ValidateProductRequest> productRequestCaptor;

    @Captor
    private ArgumentCaptor<ValidateCustomerRequest> customerRequestCaptor;

    @Test
    void shouldSendProductValidationRequest() {
        Long productId = 1L;
        Integer quantity = 5;

        CompletableFuture<ProductValidationResponse> future = validationService.validateProduct(productId, quantity);

        verify(kafkaTemplate).send(eq(KafkaConfig.VALIDATE_PRODUCT_REQUEST_TOPIC), any(String.class), productRequestCaptor.capture());
        
        ValidateProductRequest capturedRequest = productRequestCaptor.getValue();
        assertThat(capturedRequest.productId()).isEqualTo(productId);
        assertThat(capturedRequest.quantity()).isEqualTo(quantity);
        assertThat(capturedRequest.correlationId()).isNotNull();
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isFalse();
    }

    @Test
    void shouldSendCustomerValidationRequest() {
        Long customerId = 1L;

        CompletableFuture<CustomerValidationResponse> future = validationService.validateCustomer(customerId);

        verify(kafkaTemplate).send(eq(KafkaConfig.VALIDATE_CUSTOMER_REQUEST_TOPIC), any(String.class), customerRequestCaptor.capture());
        
        ValidateCustomerRequest capturedRequest = customerRequestCaptor.getValue();
        assertThat(capturedRequest.customerId()).isEqualTo(customerId);
        assertThat(capturedRequest.correlationId()).isNotNull();
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isFalse();
    }

    @Test
    void shouldCompleteProductValidationFuture() throws Exception {
        Long productId = 1L;
        CompletableFuture<ProductValidationResponse> future = validationService.validateProduct(productId, 5);

        verify(kafkaTemplate).send(eq(KafkaConfig.VALIDATE_PRODUCT_REQUEST_TOPIC), any(String.class), productRequestCaptor.capture());
        String correlationId = productRequestCaptor.getValue().correlationId();
        
        ProductValidationResponse response = new ProductValidationResponse(
            correlationId, productId, true, "Test Product", BigDecimal.TEN, null
        );

        validationService.handleProductValidationResponse(response);

        ProductValidationResponse result = future.get(1, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(response);
        assertThat(result.valid()).isTrue();
        assertThat(result.productName()).isEqualTo("Test Product");
    }

    @Test
    void shouldCompleteCustomerValidationFuture() throws Exception {
        Long customerId = 1L;
        CompletableFuture<CustomerValidationResponse> future = validationService.validateCustomer(customerId);

        verify(kafkaTemplate).send(eq(KafkaConfig.VALIDATE_CUSTOMER_REQUEST_TOPIC), any(String.class), customerRequestCaptor.capture());
        String correlationId = customerRequestCaptor.getValue().correlationId();
        
        CustomerValidationResponse response = new CustomerValidationResponse(
            correlationId, customerId, true, "John Doe", null
        );

        validationService.handleCustomerValidationResponse(response);

        CustomerValidationResponse result = future.get(1, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(response);
        assertThat(result.valid()).isTrue();
        assertThat(result.customerName()).isEqualTo("John Doe");
    }

    @Test
    void shouldHandleInvalidProduct() throws Exception {
        Long productId = 999L;
        CompletableFuture<ProductValidationResponse> future = validationService.validateProduct(productId, 5);

        verify(kafkaTemplate).send(eq(KafkaConfig.VALIDATE_PRODUCT_REQUEST_TOPIC), any(String.class), productRequestCaptor.capture());
        String correlationId = productRequestCaptor.getValue().correlationId();
        
        ProductValidationResponse response = new ProductValidationResponse(
            correlationId, productId, false, null, null, "Product not found"
        );

        validationService.handleProductValidationResponse(response);

        ProductValidationResponse result = future.get(1, TimeUnit.SECONDS);
        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Product not found");
    }

    @Test
    void shouldHandleInvalidCustomer() throws Exception {
        Long customerId = 999L;
        CompletableFuture<CustomerValidationResponse> future = validationService.validateCustomer(customerId);

        verify(kafkaTemplate).send(eq(KafkaConfig.VALIDATE_CUSTOMER_REQUEST_TOPIC), any(String.class), customerRequestCaptor.capture());
        String correlationId = customerRequestCaptor.getValue().correlationId();
        
        CustomerValidationResponse response = new CustomerValidationResponse(
            correlationId, customerId, false, null, "Customer not found"
        );

        validationService.handleCustomerValidationResponse(response);

        CustomerValidationResponse result = future.get(1, TimeUnit.SECONDS);
        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Customer not found");
    }
}

