package ru.mirea.docsa2.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.mirea.docsa2.config.KafkaConfig;
import ru.mirea.docsa2.event.OrderCreatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Sending order created event: {}", event);
        kafkaTemplate.send(KafkaConfig.ORDER_CREATED_TOPIC, event.orderId().toString(), event);
    }
}

