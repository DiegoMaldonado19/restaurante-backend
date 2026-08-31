package com.cunoc.restaurant.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Entrada de mercaderia. El costo de compra sobrescribe supply.unit_cost: es el ultimo, no el promedio. */
public record RegisterStockEntryDTO(
        @NotNull(message = "El insumo es obligatorio")
        Long supplyId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantity,

        @NotNull(message = "El costo de compra es obligatorio")
        @PositiveOrZero(message = "El costo de compra no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal purchaseCost,

        @NotNull(message = "La fecha de entrada es obligatoria")
        @PastOrPresent(message = "La fecha de entrada no puede ser futura")
        LocalDate entryDate)
{ }
