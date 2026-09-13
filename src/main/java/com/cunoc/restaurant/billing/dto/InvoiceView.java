package com.cunoc.restaurant.billing.dto;

import com.cunoc.restaurant.billing.model.Invoice;
import com.cunoc.restaurant.billing.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceView(
    Long invoiceId,
    Long invoiceNumber,
    Long tableAccountId,
    Long accountSplitId,
    BigDecimal subtotal,
    BigDecimal discountAmount,
    BigDecimal taxAmount,
    BigDecimal tipAmount,
    BigDecimal total,
    int redeemedPoints,
    int accruedPoints,
    InvoiceStatus status,
    LocalDateTime issuedAt
)
{
    public static InvoiceView from(Invoice entity)
    {
        return new InvoiceView(
            entity.getInvoiceId(),
            entity.getInvoiceNumber(),
            entity.getTableAccountId(),
            entity.getAccountSplitId(),
            entity.getSubtotal(),
            entity.getDiscountAmount(),
            entity.getTaxAmount(),
            entity.getTipAmount(),
            entity.getTotal(),
            entity.getRedeemedPoints(),
            entity.getAccruedPoints(),
            entity.getStatus(),
            entity.getIssuedAt()
        );
    }
}
