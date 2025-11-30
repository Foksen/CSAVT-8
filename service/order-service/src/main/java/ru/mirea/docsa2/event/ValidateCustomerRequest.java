package ru.mirea.docsa2.event;

public record ValidateCustomerRequest(
    String correlationId,
    Long customerId
) {}

