package com.cunoc.restaurant.ordering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitOrderDTO(
        @NotEmpty(message = "La comanda debe tener al menos un ítem")
        List<@Valid OrderLineDTO> items)
{ }
