package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.OrderItemModifier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemModifierRepository extends JpaRepository<OrderItemModifier, Long>
{
    List<OrderItemModifier> findByOrderItemIdIn(List<Long> orderItemIds);
}
