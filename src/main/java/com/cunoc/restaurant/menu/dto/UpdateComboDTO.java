package com.cunoc.restaurant.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Reemplaza la composicion completa: los platillos que trae son los que quedan. */
public record UpdateComboDTO(
        @NotBlank(message = "El nombre del combo es obligatorio")
        @Size(max = 80, message = "El nombre no puede pasar de 80 caracteres")
        String name,

        @Size(max = 255, message = "La descripcion no puede pasar de 255 caracteres")
        String description,

        @NotNull(message = "El precio del combo es obligatorio")
        @PositiveOrZero(message = "El precio del combo no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal comboPrice,

        @NotEmpty(message = "El combo debe incluir al menos un platillo")
        @Valid
        List<ComboItemDTO> items)
{ }
