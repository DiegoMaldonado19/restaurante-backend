package com.cunoc.restaurant.billing.dto;

import com.cunoc.restaurant.billing.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentDTO(
    @NotNull PaymentMethod method,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount
)
{}
