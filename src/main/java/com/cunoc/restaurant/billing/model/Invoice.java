package com.cunoc.restaurant.billing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
public class Invoice
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    private Long invoiceNumber;

    private Long tableAccountId;

    private Long accountSplitId;

    private Long restaurantTableId;

    private Long cashShiftId;

    private Long cashierId;

    private Long waiterId;

    private Long customerId;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    private int redeemedPoints;

    private int accruedPoints;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private InvoiceStatus status;

    private String voidReason;

    private LocalDateTime issuedAt;

    private LocalDateTime voidedAt;
}
