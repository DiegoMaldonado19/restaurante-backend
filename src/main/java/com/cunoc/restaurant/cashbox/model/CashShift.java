package com.cunoc.restaurant.cashbox.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_shift")
@Getter
@Setter
@NoArgsConstructor
public class CashShift
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cashShiftId;

    private Long cashierId;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal openingBalance;

    @Column(precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(precision = 12, scale = 2)
    private BigDecimal countedCash;

    @Column(precision = 12, scale = 2)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private CashShiftStatus status;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;
}
