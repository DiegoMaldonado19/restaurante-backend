package com.cunoc.restaurant.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogo de errores de negocio. Contrato entre el GlobalExceptionHandler y el
 * archivo core/error-messages.ts de las dos aplicaciones Angular: suggestedAction
 * le dice a la interfaz que boton ofrecer.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode
{
    // --- Autenticacion y autorizacion ---------------------------------------
    UNAUTHENTICATED               (HttpStatus.UNAUTHORIZED, "LOGIN"),
    INVALID_CREDENTIALS           (HttpStatus.UNAUTHORIZED, null),
    ACCOUNT_INACTIVE              (HttpStatus.FORBIDDEN,    "CONTACT_ADMIN"),
    FORBIDDEN_RESOURCE            (HttpStatus.FORBIDDEN,    null),
    USERNAME_TAKEN                (HttpStatus.CONFLICT,     null),
    CURRENT_PASSWORD_MISMATCH     (HttpStatus.CONFLICT,     null),

    // --- Caja: la regla general del enunciado -------------------------------
    CASH_SHIFT_NOT_OPEN           (HttpStatus.CONFLICT,     "OPEN_CASH_SHIFT"),
    CASH_SHIFT_ALREADY_OPEN       (HttpStatus.CONFLICT,     "CLOSE_CURRENT_SHIFT"),
    CASH_SHIFT_ALREADY_CLOSED     (HttpStatus.CONFLICT,     null),

    // --- Inventario ---------------------------------------------------------
    INSUFFICIENT_STOCK            (HttpStatus.CONFLICT,     "REMOVE_ITEM"),
    WASTE_EXCEEDS_STOCK           (HttpStatus.CONFLICT,     null),
    SUPPLY_IN_USE                 (HttpStatus.CONFLICT,     "DEACTIVATE_INSTEAD"),

    // --- Menu y recetas -----------------------------------------------------
    DISH_UNAVAILABLE              (HttpStatus.CONFLICT,     "SUGGEST_ALTERNATIVE"),
    RECIPE_NOT_DEFINED            (HttpStatus.CONFLICT,     "DEFINE_RECIPE"),
    DISH_IN_USE                   (HttpStatus.CONFLICT,     "DEACTIVATE_INSTEAD"),

    // --- Mesas y cuentas ----------------------------------------------------
    INVALID_TABLE_TRANSITION      (HttpStatus.CONFLICT,     null),
    TABLE_NOT_FREE                (HttpStatus.CONFLICT,     "CHOOSE_ANOTHER_TABLE"),
    TABLE_RESERVED                (HttpStatus.CONFLICT,     "SEAT_RESERVATION"),
    TABLE_CAPACITY_EXCEEDED       (HttpStatus.CONFLICT,     "CHOOSE_ANOTHER_TABLE"),
    ACCOUNT_ALREADY_OPEN          (HttpStatus.CONFLICT,     "OPEN_EXISTING_ACCOUNT"),
    ACCOUNT_NOT_OPEN              (HttpStatus.CONFLICT,     null),
    ACCOUNT_MERGE_INVALID         (HttpStatus.CONFLICT,     null),
    SPLIT_ITEMS_MISMATCH          (HttpStatus.CONFLICT,     null),
    SPLIT_ALREADY_INVOICED        (HttpStatus.CONFLICT,     null),

    // --- Comandas -----------------------------------------------------------
    ORDER_ITEM_IN_PREPARATION     (HttpStatus.CONFLICT,     "REQUEST_EXCEPTION"),
    INVALID_ORDER_ITEM_TRANSITION (HttpStatus.CONFLICT,     null),
    ORDER_ITEM_ALREADY_DELIVERED  (HttpStatus.CONFLICT,     null),
    ORDER_ITEM_ALREADY_CANCELLED  (HttpStatus.CONFLICT,     null),

    // --- Facturacion y fidelizacion -----------------------------------------
    ACCOUNT_HAS_PENDING_ORDERS    (HttpStatus.CONFLICT,     "DELIVER_ITEMS_FIRST"),
    ACCOUNT_ALREADY_INVOICED      (HttpStatus.CONFLICT,     "VIEW_INVOICE"),
    PAYMENT_AMOUNT_MISMATCH       (HttpStatus.CONFLICT,     null),
    INVOICE_ALREADY_VOIDED        (HttpStatus.CONFLICT,     null),
    RATING_ALREADY_SUBMITTED      (HttpStatus.CONFLICT,     null),
    INSUFFICIENT_LOYALTY_POINTS   (HttpStatus.CONFLICT,     null),
    LOYALTY_DISCOUNT_EXCEEDS_TOTAL(HttpStatus.CONFLICT,     "REDUCE_POINTS"),
    CUSTOMER_PHONE_TAKEN          (HttpStatus.CONFLICT,     "OPEN_EXISTING_CUSTOMER"),

    // --- Reservas y lista de espera -----------------------------------------
    RESERVATION_SLOT_UNAVAILABLE  (HttpStatus.CONFLICT,     "JOIN_WAITLIST"),
    RESERVATION_NOT_DUE           (HttpStatus.CONFLICT,     null),
    RESERVATION_ALREADY_CLOSED    (HttpStatus.CONFLICT,     null),
    WAITLIST_TABLE_TOO_SMALL      (HttpStatus.CONFLICT,     "CHOOSE_ANOTHER_TABLE"),

    // --- No encontrado ------------------------------------------------------
    USER_NOT_FOUND                (HttpStatus.NOT_FOUND,    null),
    SUPPLY_NOT_FOUND              (HttpStatus.NOT_FOUND,    null),
    SUPPLY_CATEGORY_NOT_FOUND     (HttpStatus.NOT_FOUND,    null),
    DISH_NOT_FOUND                (HttpStatus.NOT_FOUND,    null),
    DISH_CATEGORY_NOT_FOUND       (HttpStatus.NOT_FOUND,    null),
    MODIFIER_NOT_FOUND            (HttpStatus.NOT_FOUND,    null),
    COMBO_NOT_FOUND               (HttpStatus.NOT_FOUND,    null),
    RECIPE_NOT_FOUND              (HttpStatus.NOT_FOUND,    null),
    TABLE_NOT_FOUND               (HttpStatus.NOT_FOUND,    null),
    ACCOUNT_NOT_FOUND             (HttpStatus.NOT_FOUND,    null),
    ORDER_NOT_FOUND               (HttpStatus.NOT_FOUND,    null),
    ORDER_ITEM_NOT_FOUND          (HttpStatus.NOT_FOUND,    null),
    INVOICE_NOT_FOUND             (HttpStatus.NOT_FOUND,    null),
    CASH_SHIFT_NOT_FOUND          (HttpStatus.NOT_FOUND,    null),
    CUSTOMER_NOT_FOUND            (HttpStatus.NOT_FOUND,    null),
    RESERVATION_NOT_FOUND         (HttpStatus.NOT_FOUND,    null),
    WAITLIST_ENTRY_NOT_FOUND      (HttpStatus.NOT_FOUND,    null),

    // --- Genericos ----------------------------------------------------------
    VALIDATION_ERROR              (HttpStatus.BAD_REQUEST,  null),
    INTERNAL_ERROR                (HttpStatus.INTERNAL_SERVER_ERROR, null);

    private final HttpStatus status;
    private final String     suggestedAction;
}
