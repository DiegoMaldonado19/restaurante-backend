package com.cunoc.restaurant.dining.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
public class Reservation
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    private Long customerId;

    private Long restaurantTableId;

    private Long tableAccountId;

    private LocalDateTime reservedAt;

    private int guestCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CancellationReason cancellationReason;

    private String note;

    private LocalDateTime createdAt;
}
