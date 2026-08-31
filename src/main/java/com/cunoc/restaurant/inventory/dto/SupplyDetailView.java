package com.cunoc.restaurant.inventory.dto;

import com.cunoc.restaurant.inventory.model.StockMovement;
import com.cunoc.restaurant.inventory.model.Supply;

import java.util.List;

/** La ficha del insumo: el saldo, su valor en existencia y los ultimos movimientos. */
public record SupplyDetailView(
        SupplyView              supply,
        List<StockMovementView> recentMovements)
{
    public static SupplyDetailView from(Supply supply, List<StockMovement> movements)
    {
        return new SupplyDetailView(
                SupplyView.from(supply),
                movements.stream().map(StockMovementView::from).toList());
    }
}
