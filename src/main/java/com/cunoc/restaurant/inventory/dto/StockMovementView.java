package com.cunoc.restaurant.inventory.dto;

import com.cunoc.restaurant.inventory.model.MovementType;
import com.cunoc.restaurant.inventory.model.StockMovement;
import com.cunoc.restaurant.inventory.model.WasteReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una linea del kardex. quantity conserva el signo con el que se guardo. */
public record StockMovementView(
        Long          stockMovementId,
        Long          supplyId,
        String        supplyName,
        MovementType  movementType,
        BigDecimal    quantity,
        BigDecimal    unitCost,
        WasteReason   wasteReason,
        String        reason,
        Long          orderItemId,
        Long          userId,
        LocalDateTime createdAt)
{
    public static StockMovementView from(StockMovement movement)
    {
        return new StockMovementView(
                movement.getStockMovementId(),
                movement.getSupply().getSupplyId(),
                movement.getSupply().getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getUnitCost(),
                movement.getWasteReason(),
                movement.getReason(),
                movement.getOrderItemId(),
                movement.getUserId(),
                movement.getCreatedAt());
    }
}
