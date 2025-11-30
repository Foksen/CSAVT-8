package ru.mirea.docsa2.event;

import java.math.BigDecimal;

public record ProductValidationResponse(
    String correlationId,
    Long productId,
    boolean valid,
    String productName,
    BigDecimal price,
    String errorMessage
) {}

