package ru.mirea.docsa2.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.service.ValidationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationResponseConsumer {

    private final ValidationService validationService;

    @KafkaListener(topics = "product-validation-response", groupId = "order-service")
    public void handleProductValidationResponse(ProductValidationResponse response) {
        validationService.handleProductValidationResponse(response);
    }

    @KafkaListener(topics = "customer-validation-response", groupId = "order-service")
    public void handleCustomerValidationResponse(CustomerValidationResponse response) {
        validationService.handleCustomerValidationResponse(response);
    }
}

