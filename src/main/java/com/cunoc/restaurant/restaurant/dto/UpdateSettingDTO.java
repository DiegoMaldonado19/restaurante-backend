package com.cunoc.restaurant.restaurant.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Las cuatro columnas de restaurant_setting. @Digits cuadra el DTO con la columna:
 * DECIMAL(5,2) -> integer 3; DECIMAL(12,4) -> integer 8 (04 · §7.2).
 */
public record UpdateSettingDTO(
        @NotNull(message = "El impuesto es obligatorio")
        @PositiveOrZero(message = "El impuesto no puede ser negativo")
        @Digits(integer = 3, fraction = 2)
        BigDecimal taxPercent,

        @NotNull(message = "La propina sugerida es obligatoria")
        @PositiveOrZero(message = "La propina sugerida no puede ser negativa")
        @Digits(integer = 3, fraction = 2)
        BigDecimal tipSuggestedPercent,

        @NotNull(message = "Los puntos por unidad de moneda son obligatorios")
        @PositiveOrZero(message = "Los puntos por unidad de moneda no pueden ser negativos")
        @Digits(integer = 8, fraction = 4)
        BigDecimal pointsPerCurrencyUnit,

        @NotNull(message = "El valor del punto es obligatorio")
        @PositiveOrZero(message = "El valor del punto no puede ser negativo")
        @Digits(integer = 8, fraction = 4)
        BigDecimal currencyPerPoint)
{ }