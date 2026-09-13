package com.cunoc.restaurant.cashbox.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpenCashShiftDTO(
    @NotNull @DecimalMin("0.0") BigDecimal openingBalance
)
{}
