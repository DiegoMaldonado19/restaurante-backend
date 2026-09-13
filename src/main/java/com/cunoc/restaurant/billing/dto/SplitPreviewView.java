package com.cunoc.restaurant.billing.dto;

import java.math.BigDecimal;

public record SplitPreviewView(
    Long accountSplitId,
    String label,
    BigDecimal subtotal
)
{}
