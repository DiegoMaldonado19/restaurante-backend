package com.cunoc.restaurant.iam.model;

/** Un empleado nunca se borra: hay cuentas, comandas y facturas que lo referencian. */
public enum UserStatus
{
    ACTIVE,
    INACTIVE
}
