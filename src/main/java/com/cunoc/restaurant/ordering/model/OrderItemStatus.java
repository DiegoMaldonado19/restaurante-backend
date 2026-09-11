package com.cunoc.restaurant.ordering.model;

/**
 * Estado de cada platillo de una ronda. RECEIVED -> IN_PREPARATION -> READY -> DELIVERED;
 * UNAVAILABLE (cocina) y CANCELLED (excepcion de admin) son salidas. El estado de la ronda
 * es el minimo de los estados de sus items, no una columna.
 */
public enum OrderItemStatus
{
    RECEIVED,
    IN_PREPARATION,
    READY,
    DELIVERED,
    UNAVAILABLE,
    CANCELLED
}