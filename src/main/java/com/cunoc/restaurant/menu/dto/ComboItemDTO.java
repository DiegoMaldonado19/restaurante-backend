package com.cunoc.restaurant.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Una linea de la composicion del combo: un platillo y cuantas veces entra. */
public record ComboItemDTO(
        @NotNull(message = "El platillo es obligatorio")
        Long dishId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        Integer quantity)
{ }
