package com.cunoc.restaurant.billing.dto;

import com.cunoc.restaurant.ordering.dto.OrderItemView;

import java.math.BigDecimal;

/**
 * Una linea del comprobante: el platillo tal como se vendio, con el precio congelado al
 * enviar la comanda. lineTotal es unitPrice x quantity, que es exactamente lo que
 * billPreview suma al subtotal, para que el comprobante cuadre con la factura.
 */
public record InvoiceLineView(
    String     dishName,
    int        quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    String     note)
{
    public static InvoiceLineView of(OrderItemView item, String dishName)
    {
        return new InvoiceLineView(
            dishName,
            item.quantity(),
            item.unitPrice(),
            item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())),
            item.note());
    }
}
