package ru.mirea.docsa2.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
    Long orderId,
    Long customerId,
    Long productId,
    Integer quantity,
    BigDecimal totalPrice,
    LocalDateTime createdAt
) {}

