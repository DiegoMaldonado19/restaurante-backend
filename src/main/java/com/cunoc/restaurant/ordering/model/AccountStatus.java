package com.cunoc.restaurant.ordering.model;

/**
 * Estados de la cuenta de mesa. CLOSED, MERGED y CANCELLED son terminales; billing cierra
 * la cuenta al facturar, y la fusion y la anulacion ocurren dentro de ordering.
 */
public enum AccountStatus
{
    OPEN,
    BILL_REQUESTED,
    CLOSED,
    MERGED,
    CANCELLED
}