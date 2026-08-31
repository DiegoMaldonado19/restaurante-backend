package com.cunoc.restaurant.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplyCategoryDTO(
        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 60, message = "El nombre no puede pasar de 60 caracteres")
        String name)
{ }
