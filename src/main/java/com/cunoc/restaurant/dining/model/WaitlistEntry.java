package com.cunoc.restaurant.dining.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entry")
@Getter
@Setter
@NoArgsConstructor
public class WaitlistEntry
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long waitlistEntryId;

    private Long customerId;

    private int guestCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private WaitlistStatus status;

    private Long restaurantTableId;

    private Long tableAccountId;

    private LocalDateTime arrivedAt;

    private LocalDateTime seatedAt;
}
