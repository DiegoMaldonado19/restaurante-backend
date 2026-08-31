package com.cunoc.restaurant.inventory.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSupplyStatusDTO(
        @NotNull(message = "El estado es obligatorio")
        Boolean active)
{ }
