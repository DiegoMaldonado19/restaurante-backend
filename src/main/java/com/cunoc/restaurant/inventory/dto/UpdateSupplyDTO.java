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

/** Sin unitCost ni currentStock: el costo lo fija una entrada y el saldo, un movimiento. */
@ConsistentStockThresholds
public record UpdateSupplyDTO(
        @NotNull(message = "La categoria es obligatoria")
        Long supplyCategoryId,

        @NotBlank(message = "El nombre del insumo es obligatorio")
        @Size(max = 80, message = "El nombre no puede pasar de 80 caracteres")
        String name,

        @NotNull(message = "La unidad de medida es obligatoria")
        MeasureUnit measureUnit,

        @NotNull(message = "El stock minimo es obligatorio")
        @PositiveOrZero(message = "El stock minimo no puede ser negativo")
        @Digits(integer = 9, fraction = 3)
        BigDecimal minStock,

        @PositiveOrZero(message = "El stock maximo no puede ser negativo")
        @Digits(integer = 9, fraction = 3)
        BigDecimal maxStock)
        implements StockThresholds
{ }
