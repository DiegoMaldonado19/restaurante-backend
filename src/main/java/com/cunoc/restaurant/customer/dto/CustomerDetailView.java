package com.cunoc.restaurant.customer.dto;

import com.cunoc.restaurant.customer.model.Customer;

import java.time.LocalDateTime;

/**
 * La ficha del cliente. visitCount sale del propio libro mayor: hay una acreditacion
 * por factura. El consumo acumulado vive en invoice.total, que es de billing, y se
 * agrega cuando ese modulo exista.
 */
public record CustomerDetailView(
        Long          customerId,
        String        fullName,
        String        phone,
        LocalDateTime createdAt,
        int           availablePoints,
        long          visitCount)
{
    public static CustomerDetailView from(Customer customer, int availablePoints, long visitCount)
    {
        return new CustomerDetailView(
                customer.getCustomerId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getCreatedAt(),
                availablePoints,
                visitCount);
    }
}
