package ru.mirea.docsa2.event;

public record CustomerValidationResponse(
    String correlationId,
    Long customerId,
    boolean valid,
    String customerName,
    String errorMessage
) {}

