package com.cunoc.restaurant.menu.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateModifierDTO(
        @NotBlank(message = "El nombre del modificador es obligatorio")
        @Size(max = 60, message = "El nombre no puede pasar de 60 caracteres")
        String name,

        @NotNull(message = "El costo adicional es obligatorio")
        @PositiveOrZero(message = "El costo adicional no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal extraPrice)
{ }
