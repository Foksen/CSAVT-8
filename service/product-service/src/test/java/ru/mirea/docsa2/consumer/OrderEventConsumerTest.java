package ru.mirea.docsa2.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mirea.docsa2.event.OrderCreatedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void shouldHandleOrderCreatedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
            1L, 2L, 3L, 5, BigDecimal.valueOf(100), LocalDateTime.now()
        );

        assertDoesNotThrow(() -> consumer.handleOrderCreated(event));
    }
}

