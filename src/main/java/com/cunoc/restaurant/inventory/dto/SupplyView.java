package com.cunoc.restaurant.inventory.dto;

import com.cunoc.restaurant.inventory.model.MeasureUnit;
import com.cunoc.restaurant.inventory.model.Supply;

import java.math.BigDecimal;

public record SupplyView(
        Long        supplyId,
        Long        supplyCategoryId,
        String      categoryName,
        String      name,
        MeasureUnit measureUnit,
        BigDecimal  unitCost,
        BigDecimal  currentStock,
        BigDecimal  minStock,
        BigDecimal  maxStock,
        BigDecimal  stockValue,
        boolean     lowStock,
        boolean     active)
{
    public static SupplyView from(Supply supply)
    {
        return new SupplyView(
                supply.getSupplyId(),
                supply.getCategory().getSupplyCategoryId(),
                supply.getCategory().getName(),
                supply.getName(),
                supply.getMeasureUnit(),
                supply.getUnitCost(),
                supply.getCurrentStock(),
                supply.getMinStock(),
                supply.getMaxStock(),
                supply.stockValue(),
                supply.isLowStock(),
                supply.isActive());
    }
}
