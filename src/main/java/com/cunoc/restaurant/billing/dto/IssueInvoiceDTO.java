package com.cunoc.restaurant.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * accountSplitId nulo factura la cuenta completa; con valor factura solo esa sub-cuenta.
 * customerId y redeemPoints son opcionales: sin cliente no hay fidelizacion en esta venta.
 * payments es una lista para soportar pagos combinados (efectivo + tarjeta) o, si se
 * factura por sub-cuenta, la division entre varias personas.
 * tipAmount es la propina realmente cobrada; nula equivale a cero. La sugerida viaja
 * en la precuenta, pero quien decide es el cliente en el mostrador.
 */
public record IssueInvoiceDTO(
    Long accountSplitId,
    @NotEmpty @Valid List<PaymentDTO> payments,
    Long customerId,
    Integer redeemPoints,
    @PositiveOrZero BigDecimal tipAmount
)
{}
