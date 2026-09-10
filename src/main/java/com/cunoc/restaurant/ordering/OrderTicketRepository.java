package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.ordering.model.OrderTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTicketRepository extends JpaRepository<OrderTicket, Long>
{
}
