package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.validation.ExactlyOneProduct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@ExactlyOneProduct
public record OrderLineDTO(
        Long dishId,

        Long comboId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Max(value = 99, message = "La cantidad máxima es 99")
        Integer quantity,

        List<Long> modifierIds,

        @Size(max = 255, message = "La nota no puede superar los 255 caracteres")
        String note)
{ }
