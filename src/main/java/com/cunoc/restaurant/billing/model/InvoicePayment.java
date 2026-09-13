package com.cunoc.restaurant.billing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_payment")
@Getter
@Setter
@NoArgsConstructor
public class InvoicePayment
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoicePaymentId;

    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private PaymentMethod method;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;
}
