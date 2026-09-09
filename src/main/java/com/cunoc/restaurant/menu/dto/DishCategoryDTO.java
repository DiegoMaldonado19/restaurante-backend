package com.cunoc.restaurant.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DishCategoryDTO(
        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 60, message = "El nombre no puede pasar de 60 caracteres")
        String name,

        @NotNull(message = "El orden de despliegue es obligatorio")
        @PositiveOrZero(message = "El orden de despliegue no puede ser negativo")
        Integer displayOrder)
{ }
