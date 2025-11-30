package ru.mirea.docsa2.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.ValidateCustomerRequest;
import ru.mirea.docsa2.repository.CustomerRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerValidationConsumer {

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "validate-customer-request", groupId = "customer-service")
    public void handleValidateCustomerRequest(ValidateCustomerRequest request) {
        log.info("Received customer validation request: {}", request);

        var customer = customerRepository.findById(request.customerId());

        CustomerValidationResponse response;
        if (customer.isEmpty()) {
            response = new CustomerValidationResponse(
                request.correlationId(),
                request.customerId(),
                false,
                null,
                "Customer not found"
            );
        } else {
            response = new CustomerValidationResponse(
                request.correlationId(),
                request.customerId(),
                true,
                customer.get().getName(),
                null
            );
        }

        log.info("Sending customer validation response: {}", response);
        kafkaTemplate.send("customer-validation-response", request.correlationId(), response);
    }
}

