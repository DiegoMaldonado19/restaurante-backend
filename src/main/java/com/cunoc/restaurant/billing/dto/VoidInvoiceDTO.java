package com.cunoc.restaurant.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidInvoiceDTO(
    @NotBlank @Size(max = 255) String reason
)
{}
