package com.cunoc.restaurant.customer.model;

/** Las dos unicas formas de mover el saldo, y ambas ocurren dentro de POST /invoices. */
public enum LoyaltyTransactionType
{
    ACCRUAL,
    REDEMPTION
}
