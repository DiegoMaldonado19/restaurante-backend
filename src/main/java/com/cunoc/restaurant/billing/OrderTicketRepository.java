package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.OrderTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderTicketRepository extends JpaRepository<OrderTicket, Long>
{
    List<OrderTicket> findByTableAccountId(Long tableAccountId);
}
