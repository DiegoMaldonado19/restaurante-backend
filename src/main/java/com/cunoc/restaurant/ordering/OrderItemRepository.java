package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.ordering.model.AccountSplit;
import com.cunoc.restaurant.ordering.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>
{
    @Query("""
           SELECT i FROM OrderItem i
            WHERE i.split.account.tableAccountId = :accountId
           """)
    Page<OrderItem> findByAccountSplitTableAccountId(@Param("accountId") Long accountId,
                                                    Pageable pageable);

    List<OrderItem> findBySplitAccountSplitId(@Param("splitId") Long splitId);
}
