package ru.mirea.docsa2.event;

public record ValidateProductRequest(
    String correlationId,
    Long productId,
    Integer quantity
) {}

