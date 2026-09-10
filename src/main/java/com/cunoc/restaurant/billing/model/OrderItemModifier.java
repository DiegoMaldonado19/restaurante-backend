package com.cunoc.restaurant.billing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item_modifier")
@Getter
@NoArgsConstructor
public class OrderItemModifier
{
    @Id
    private Long orderItemModifierId;

    private Long orderItemId;

    private BigDecimal extraPrice;
}
