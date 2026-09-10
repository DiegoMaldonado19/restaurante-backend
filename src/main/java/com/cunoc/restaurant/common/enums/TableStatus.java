package com.cunoc.restaurant.common.enums;

/**
 * Estados de la mesa: exactamente los cuatro del enunciado. Vive en common porque lo usan
 * restaurant (unico escritor via transitionTo), dining (panel de ocupacion), ordering
 * (abrir cuenta, transferir, pedir la cuenta) y billing (liberar mesa al cobrar).
 */
public enum TableStatus
{
    FREE,
    RESERVED,
    OCCUPIED,
    BILL_REQUESTED
}