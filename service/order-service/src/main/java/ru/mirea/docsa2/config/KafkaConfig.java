package ru.mirea.docsa2.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order-created";
    public static final String VALIDATE_PRODUCT_REQUEST_TOPIC = "validate-product-request";
    public static final String PRODUCT_VALIDATION_RESPONSE_TOPIC = "product-validation-response";
    public static final String VALIDATE_CUSTOMER_REQUEST_TOPIC = "validate-customer-request";
    public static final String CUSTOMER_VALIDATION_RESPONSE_TOPIC = "customer-validation-response";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic validateProductRequestTopic() {
        return TopicBuilder.name(VALIDATE_PRODUCT_REQUEST_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic productValidationResponseTopic() {
        return TopicBuilder.name(PRODUCT_VALIDATION_RESPONSE_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic validateCustomerRequestTopic() {
        return TopicBuilder.name(VALIDATE_CUSTOMER_REQUEST_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic customerValidationResponseTopic() {
        return TopicBuilder.name(CUSTOMER_VALIDATION_RESPONSE_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
}

