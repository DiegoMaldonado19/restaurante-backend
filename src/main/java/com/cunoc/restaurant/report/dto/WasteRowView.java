package com.cunoc.restaurant.report.dto;

import com.cunoc.restaurant.inventory.model.MeasureUnit;
import com.cunoc.restaurant.inventory.model.WasteReason;

import java.math.BigDecimal;

/** Mermas del periodo por insumo y motivo. La cantidad se reporta en positivo. */
public record WasteRowView(
        Long        supplyId,
        String      supplyName,
        String      categoryName,
        WasteReason wasteReason,
        MeasureUnit measureUnit,
        BigDecimal  quantity,
        BigDecimal  lostCost,
        Long        movements)
{
}
