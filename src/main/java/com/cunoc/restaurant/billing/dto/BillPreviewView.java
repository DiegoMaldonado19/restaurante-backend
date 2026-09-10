package com.cunoc.restaurant.billing.dto;

import java.math.BigDecimal;
import java.util.List;

public record BillPreviewView(
    Long accountId,
    BigDecimal subtotal,
    BigDecimal taxPercent,
    BigDecimal taxAmount,
    BigDecimal suggestedTipPercent,
    BigDecimal suggestedTipAmount,
    BigDecimal total,
    List<SplitPreviewView> splits
)
{}
