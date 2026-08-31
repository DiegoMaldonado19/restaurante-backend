package com.cunoc.restaurant.customer.dto;

import com.cunoc.restaurant.customer.model.LoyaltyTransaction;
import com.cunoc.restaurant.customer.model.LoyaltyTransactionType;

import java.time.LocalDateTime;

public record LoyaltyTransactionView(
        Long                   loyaltyTransactionId,
        LoyaltyTransactionType transactionType,
        int                    points,
        Long                   invoiceId,
        LocalDateTime          createdAt)
{
    public static LoyaltyTransactionView from(LoyaltyTransaction transaction)
    {
        return new LoyaltyTransactionView(
                transaction.getLoyaltyTransactionId(),
                transaction.getTransactionType(),
                transaction.getPoints(),
                transaction.getInvoiceId(),
                transaction.getCreatedAt());
    }
}
