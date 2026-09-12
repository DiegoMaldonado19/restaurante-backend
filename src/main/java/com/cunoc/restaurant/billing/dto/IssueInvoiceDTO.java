package com.cunoc.restaurant.billing.dto;

import com.cunoc.restaurant.billing.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/**
 * accountSplitId nulo factura la cuenta completa; con valor factura solo esa sub-cuenta.
 * customerId y redeemPoints son opcionales: sin cliente no hay fidelizacion en esta venta.
 */
public record IssueInvoiceDTO(
    Long accountSplitId,
    @NotNull PaymentMethod paymentMethod,
    Long customerId,
    Integer redeemPoints
)
{}
