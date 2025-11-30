package ru.mirea.docsa2.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.service.ValidationService;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ValidationResponseConsumerTest {

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private ValidationResponseConsumer consumer;

    @Test
    void shouldHandleProductValidationResponse() {
        ProductValidationResponse response = new ProductValidationResponse(
            "corr-123", 1L, true, "Product", BigDecimal.TEN, null
        );

        consumer.handleProductValidationResponse(response);

        verify(validationService).handleProductValidationResponse(response);
    }

    @Test
    void shouldHandleCustomerValidationResponse() {
        CustomerValidationResponse response = new CustomerValidationResponse(
            "corr-456", 1L, true, "Customer", null
        );

        consumer.handleCustomerValidationResponse(response);

        verify(validationService).handleCustomerValidationResponse(response);
    }
}

