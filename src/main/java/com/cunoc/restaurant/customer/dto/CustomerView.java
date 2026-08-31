package com.cunoc.restaurant.customer.dto;

import com.cunoc.restaurant.customer.model.Customer;

import java.time.LocalDateTime;

public record CustomerView(
        Long          customerId,
        String        fullName,
        String        phone,
        LocalDateTime createdAt)
{
    public static CustomerView from(Customer customer)
    {
        return new CustomerView(
                customer.getCustomerId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getCreatedAt());
    }
}
