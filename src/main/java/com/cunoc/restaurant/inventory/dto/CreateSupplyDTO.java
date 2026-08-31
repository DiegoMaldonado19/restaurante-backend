package com.cunoc.restaurant.inventory.dto;

import com.cunoc.restaurant.inventory.model.MeasureUnit;
import com.cunoc.restaurant.inventory.validation.ConsistentStockThresholds;
import com.cunoc.restaurant.inventory.validation.StockThresholds;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** @Digits cuadra con la columna: DECIMAL(12,2) -> integer 10, DECIMAL(12,3) -> integer 9. */
@ConsistentStockThresholds
public record CreateSupplyDTO(
        @NotNull(message = "La categoria es obligatoria")
        Long supplyCategoryId,

        @NotBlank(message = "El nombre del insumo es obligatorio")
        @Size(max = 80, message = "El nombre no puede pasar de 80 caracteres")
        String name,

        @NotNull(message = "La unidad de medida es obligatoria")
        MeasureUnit measureUnit,

        @NotNull(message = "El costo unitario es obligatorio")
        @PositiveOrZero(message = "El costo unitario no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal unitCost,

        @NotNull(message = "El stock minimo es obligatorio")
        @PositiveOrZero(message = "El stock minimo no puede ser negativo")
        @Digits(integer = 9, fraction = 3)
        BigDecimal minStock,

        @PositiveOrZero(message = "El stock maximo no puede ser negativo")
        @Digits(integer = 9, fraction = 3)
        BigDecimal maxStock)
        implements StockThresholds
{ }
