package ru.mirea.docsa2.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.event.ValidateProductRequest;
import ru.mirea.docsa2.repository.ProductRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidationConsumer {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "validate-product-request", groupId = "product-service")
    public void handleValidateProductRequest(ValidateProductRequest request) {
        log.info("Received product validation request: {}", request);

        var product = productRepository.findById(request.productId());

        ProductValidationResponse response;
        if (product.isEmpty()) {
            response = new ProductValidationResponse(
                request.correlationId(),
                request.productId(),
                false,
                null,
                null,
                "Product not found"
            );
        } else {
            response = new ProductValidationResponse(
                request.correlationId(),
                request.productId(),
                true,
                product.get().getName(),
                product.get().getPrice(),
                null
            );
        }

        log.info("Sending product validation response: {}", response);
        kafkaTemplate.send("product-validation-response", request.correlationId(), response);
    }
}

