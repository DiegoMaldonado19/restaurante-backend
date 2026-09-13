package com.cunoc.restaurant.billing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_rating")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRating
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long serviceRatingId;

    private Long invoiceId;

    private Long waiterId;

    private int score;

    private String commentText;

    private LocalDateTime createdAt;
}
