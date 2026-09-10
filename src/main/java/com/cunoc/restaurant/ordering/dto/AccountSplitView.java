package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.AccountSplit;
import com.cunoc.restaurant.ordering.model.SplitMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AccountSplitView(
        Long accountSplitId,
        String label,
        SplitMode mode,
        BigDecimal shareAmount,
        LocalDateTime createdAt,
        List<OrderItemView> items)
{
    public static AccountSplitView from(AccountSplit split)
    {
        return new AccountSplitView(
                split.getAccountSplitId(),
                split.getLabel(),
                split.getMode(),
                split.getShareAmount(),
                split.getCreatedAt(),
                List.of());
    }
}
