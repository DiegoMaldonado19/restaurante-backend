package com.cunoc.restaurant.ordering.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderLineDTO(
        @NotNull(message = "El identificador del platillo es obligatorio")
        Long dishId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Max(value = 99, message = "La cantidad máxima es 99")
        Integer quantity,

        List<Long> modifierIds)
{ }
