package com.cunoc.restaurant.report.dto;

import com.cunoc.restaurant.inventory.model.MeasureUnit;

import java.math.BigDecimal;

/** Existencias valoradas al costo actual del insumo, y la bandera de bajo minimo. */
public record InventoryRowView(
        Long        supplyId,
        String      name,
        String      categoryName,
        MeasureUnit measureUnit,
        BigDecimal  currentStock,
        BigDecimal  minStock,
        BigDecimal  maxStock,
        BigDecimal  unitCost,
        BigDecimal  stockValue,
        Boolean     lowStock)
{
}
