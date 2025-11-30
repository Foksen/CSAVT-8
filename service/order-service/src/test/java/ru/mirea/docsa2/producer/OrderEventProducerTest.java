package ru.mirea.docsa2.producer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.mirea.docsa2.config.KafkaConfig;
import ru.mirea.docsa2.event.OrderCreatedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer producer;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;

    @Test
    void shouldSendOrderCreatedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
            1L, 2L, 3L, 5, BigDecimal.valueOf(100), LocalDateTime.now()
        );

        producer.sendOrderCreatedEvent(event);

        verify(kafkaTemplate).send(eq(KafkaConfig.ORDER_CREATED_TOPIC), eq("1"), eventCaptor.capture());
        
        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.orderId()).isEqualTo(1L);
        assertThat(capturedEvent.customerId()).isEqualTo(2L);
        assertThat(capturedEvent.productId()).isEqualTo(3L);
        assertThat(capturedEvent.quantity()).isEqualTo(5);
        assertThat(capturedEvent.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}

