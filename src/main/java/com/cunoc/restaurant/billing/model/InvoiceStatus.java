package com.cunoc.restaurant.billing.model;

/**
 * Estado de la factura. ISSUED es el estado normal tras facturar; VOIDED es la
 * anulación (queda constancia con voidReason y voidedAt, nunca se borra el registro).
 */
public enum InvoiceStatus
{
    ISSUED,
    VOIDED
}
