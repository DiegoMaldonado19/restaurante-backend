package com.cunoc.restaurant.cashbox.dto;

import com.cunoc.restaurant.cashbox.model.CashMovement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashMovementView(
    Long cashMovementId,
    Long cashShiftId,
    String movementType,
    BigDecimal amount,
    Long invoiceId,
    LocalDateTime createdAt
)
{
    public static CashMovementView from(CashMovement entity)
    {
        return new CashMovementView(
            entity.getCashMovementId(),
            entity.getCashShiftId(),
            entity.getMovementType().name(),
            entity.getAmount(),
            entity.getInvoiceId(),
            entity.getCreatedAt()
        );
    }
}
