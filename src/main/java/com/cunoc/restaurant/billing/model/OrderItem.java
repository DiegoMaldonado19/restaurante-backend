package com.cunoc.restaurant.billing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor
public class OrderItem
{
    @Id
    private Long orderItemId;

    private Long orderTicketId;

    private Long accountSplitId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private String status;
}
