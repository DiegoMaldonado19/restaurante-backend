package com.cunoc.restaurant.inventory.model;

/** Los cuatro origenes de un movimiento de stock. El signo lo aplica applyMovement(). */
public enum MovementType
{
    PURCHASE,
    SALE,
    WASTE,
    ADJUSTMENT
}
