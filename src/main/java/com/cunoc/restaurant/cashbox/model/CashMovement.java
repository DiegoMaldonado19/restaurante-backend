package com.cunoc.restaurant.cashbox.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_movement")
@Getter
@Setter
@NoArgsConstructor
public class CashMovement
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cashMovementId;

    private Long cashShiftId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MovementType movementType;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    private Long invoiceId;

    private LocalDateTime createdAt;
}
