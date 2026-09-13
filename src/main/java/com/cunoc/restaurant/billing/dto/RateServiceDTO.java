package com.cunoc.restaurant.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RateServiceDTO(
    @NotNull @Min(1) @Max(5) Integer score,
    @Size(max = 255) String commentText
)
{}
