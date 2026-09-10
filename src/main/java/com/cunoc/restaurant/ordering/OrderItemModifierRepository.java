package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.ordering.model.OrderItemModifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemModifierRepository extends JpaRepository<OrderItemModifier, Long>
{
    List<OrderItemModifier> findByOrderItemOrderItemId(@Param("orderItemId") Long orderItemId);
}
