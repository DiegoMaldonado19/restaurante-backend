package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>
{
    @Query("""
           SELECT i FROM OrderItem i
            WHERE i.split.account.tableAccountId = :accountId
           """)
    Page<OrderItem> findByAccountSplitTableAccountId(@Param("accountId") Long accountId,
                                                    Pageable pageable);

    List<OrderItem> findBySplitAccountSplitId(@Param("splitId") Long splitId);

    @Query("""
           SELECT i FROM OrderItem i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:tableId IS NULL OR i.ticket.account.restaurantTableId = :tableId)
              AND (:waiterId IS NULL OR i.ticket.waiterId = :waiterId)
              AND i.ticket.account.status IN (com.cunoc.restaurant.ordering.model.AccountStatus.OPEN,
                                              com.cunoc.restaurant.ordering.model.AccountStatus.BILL_REQUESTED)
           ORDER BY i.submittedAt ASC
           """)
    Page<OrderItem> searchQueue(@Param("status") OrderItemStatus status,
                                @Param("tableId") Long tableId,
                                @Param("waiterId") Long waiterId,
                                Pageable pageable);

    /**
     * Lectura para escribir el item. FOR UPDATE no va en el HQL: Hibernate lo agrega
     * al SQL a partir de @Lock, igual que en TableAccountRepository y SupplyRepository.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM OrderItem i WHERE i.orderItemId = :id")
    Optional<OrderItem> findByIdForUpdate(@Param("id") Long id);
}
