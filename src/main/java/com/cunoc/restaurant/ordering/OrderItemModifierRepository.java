package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.ordering.model.OrderItemModifier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemModifierRepository extends JpaRepository<OrderItemModifier, Long>
{
}
