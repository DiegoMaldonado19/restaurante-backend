package com.cunoc.restaurant.menu.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateComboStatusDTO(
        @NotNull(message = "El estado es obligatorio")
        Boolean active)
{ }
