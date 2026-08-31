package com.cunoc.restaurant.inventory.dto;

import com.cunoc.restaurant.inventory.model.WasteReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Merma. El motivo enumerado es obligatorio; la nota libre amplia el "por que". */
public record RegisterStockWasteDTO(
        @NotNull(message = "El insumo es obligatorio")
        Long supplyId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantity,

        @NotNull(message = "El motivo de la merma es obligatorio")
        WasteReason wasteReason,

        @Size(max = 255, message = "La nota no puede pasar de 255 caracteres")
        String reason)
{ }
