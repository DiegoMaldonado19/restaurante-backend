package com.cunoc.restaurant.report.dto;

import java.math.BigDecimal;

/**
 * Descuadre entre la columna current_stock y el libro mayor de movimientos. La consulta
 * que la alimenta debe devolver cero filas: una fila aqui es un error de contabilidad
 * de inventario, no un dato de negocio.
 */
public record StockReconciliationRowView(
        Long       supplyId,
        String     name,
        BigDecimal currentStock,
        BigDecimal ledgerBalance)
{
}
