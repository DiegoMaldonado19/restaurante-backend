package com.cunoc.restaurant.menu.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** No toca la disponibilidad manual ni el estado: cada uno tiene su propio endpoint. */
public record UpdateDishDTO(
        @NotNull(message = "La categoria es obligatoria")
        Long dishCategoryId,

        @NotBlank(message = "El nombre del platillo es obligatorio")
        @Size(max = 80, message = "El nombre no puede pasar de 80 caracteres")
        String name,

        @Size(max = 255, message = "La descripcion no puede pasar de 255 caracteres")
        String description,

        @NotNull(message = "El precio de venta es obligatorio")
        @PositiveOrZero(message = "El precio de venta no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal salePrice,

        @Size(max = 255, message = "La URL de la imagen no puede pasar de 255 caracteres")
        String imageUrl,

        @NotNull(message = "El tiempo estimado de preparacion es obligatorio")
        @PositiveOrZero(message = "El tiempo de preparacion no puede ser negativo")
        Integer prepMinutes)
{ }
