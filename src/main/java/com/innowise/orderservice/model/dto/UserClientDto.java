package com.innowise.orderservice.model.dto;

import java.time.Instant;
import java.time.LocalDate;

public record UserClientDto(
        Long id,
        String name,
        String surname,
        String email,
        boolean active,
        LocalDate birthDate,
        Instant createdAt,
        Instant updatedAt
) {
}
