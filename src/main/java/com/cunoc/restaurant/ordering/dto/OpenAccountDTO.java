package com.cunoc.restaurant.ordering.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OpenAccountDTO(
        @NotNull(message = "El identificador de mesa es obligatorio")
        Long tableId,

        @NotNull(message = "El número de comensales es obligatorio")
        @Positive(message = "El número de comensales debe ser mayor que cero")
        @Max(value = 50, message = "El número máximo de comensales permitido es 50")
        Integer guestCount)
{ }
