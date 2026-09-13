package com.cunoc.restaurant.billing.dto;

import com.cunoc.restaurant.billing.model.Invoice;
import com.cunoc.restaurant.billing.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La factura. `items` y `restaurantTableNumber` solo vienen resueltos en GET /invoices/{id},
 * que es el comprobante imprimible; el historial los deja vacios para no hacer una lectura
 * de cuenta y otra de mesa por cada fila de la pagina.
 */
public record InvoiceView(
    Long invoiceId,
    Long invoiceNumber,
    Long tableAccountId,
    Long accountSplitId,
    Long restaurantTableId,
    Integer restaurantTableNumber,
    BigDecimal subtotal,
    BigDecimal discountAmount,
    BigDecimal taxAmount,
    BigDecimal tipAmount,
    BigDecimal total,
    int redeemedPoints,
    int accruedPoints,
    InvoiceStatus status,
    String voidReason,
    LocalDateTime issuedAt,
    List<InvoiceLineView> items
)
{
    public static InvoiceView from(Invoice entity)
    {
        return of(entity, List.of(), null);
    }

    public static InvoiceView of(Invoice entity, List<InvoiceLineView> items, Integer tableNumber)
    {
        return new InvoiceView(
            entity.getInvoiceId(),
            entity.getInvoiceNumber(),
            entity.getTableAccountId(),
            entity.getAccountSplitId(),
            entity.getRestaurantTableId(),
            tableNumber,
            entity.getSubtotal(),
            entity.getDiscountAmount(),
            entity.getTaxAmount(),
            entity.getTipAmount(),
            entity.getTotal(),
            entity.getRedeemedPoints(),
            entity.getAccruedPoints(),
            entity.getStatus(),
            entity.getVoidReason(),
            entity.getIssuedAt(),
            items
        );
    }
}
