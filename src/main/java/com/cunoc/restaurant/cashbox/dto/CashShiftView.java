package com.cunoc.restaurant.cashbox.dto;

import com.cunoc.restaurant.cashbox.model.CashShift;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashShiftView(
    Long cashShiftId,
    Long cashierId,
    BigDecimal openingBalance,
    BigDecimal expectedCash,
    BigDecimal countedCash,
    BigDecimal difference,
    String status,
    LocalDateTime openedAt,
    LocalDateTime closedAt
)
{
    public static CashShiftView from(CashShift entity)
    {
        return new CashShiftView(
            entity.getCashShiftId(),
            entity.getCashierId(),
            entity.getOpeningBalance(),
            entity.getExpectedCash(),
            entity.getCountedCash(),
            entity.getDifference(),
            entity.getStatus().name(),
            entity.getOpenedAt(),
            entity.getClosedAt()
        );
    }
}
