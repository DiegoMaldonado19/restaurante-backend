package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>
{
    List<OrderItem> findByOrderTicketIdIn(List<Long> orderTicketIds);
}
