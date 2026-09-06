package com.cunoc.restaurant.menu.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Una linea de receta: un insumo y la cantidad exacta que consume. quantity DECIMAL(12,3). */
public record RecipeItemDTO(
        @NotNull(message = "El insumo es obligatorio")
        Long supplyId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantity)
{ }
