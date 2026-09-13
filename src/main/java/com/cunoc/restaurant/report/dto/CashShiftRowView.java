package com.cunoc.restaurant.report.dto;

import com.cunoc.restaurant.cashbox.model.CashShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cuadres del periodo. cash_shift ya guarda esperado, contado y diferencia. */
public record CashShiftRowView(
        Long            cashShiftId,
        Long            cashierId,
        String          cashierName,
        LocalDateTime   openedAt,
        LocalDateTime   closedAt,
        BigDecimal      openingBalance,
        BigDecimal      expectedCash,
        BigDecimal      countedCash,
        BigDecimal      difference,
        CashShiftStatus status)
{
}
