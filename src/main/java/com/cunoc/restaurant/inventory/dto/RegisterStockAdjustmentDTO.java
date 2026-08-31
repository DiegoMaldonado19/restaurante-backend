package com.cunoc.restaurant.inventory.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Ajuste manual. Es el unico movimiento que admite signo en la peticion. */
public record RegisterStockAdjustmentDTO(
        @NotNull(message = "El insumo es obligatorio")
        Long supplyId,

        @NotNull(message = "La cantidad es obligatoria")
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantity,

        @NotBlank(message = "El motivo del ajuste es obligatorio")
        @Size(max = 255, message = "El motivo no puede pasar de 255 caracteres")
        String reason)
{ }
