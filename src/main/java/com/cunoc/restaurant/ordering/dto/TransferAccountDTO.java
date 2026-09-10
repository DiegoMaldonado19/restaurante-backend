package com.cunoc.restaurant.ordering.dto;

import jakarta.validation.constraints.NotNull;

public record TransferAccountDTO(
        @NotNull(message = "El identificador de mesa destino es obligatorio")
        Long targetTableId)
{ }
