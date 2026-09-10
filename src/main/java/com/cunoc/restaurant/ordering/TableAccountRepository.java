package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.TableAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface TableAccountRepository extends JpaRepository<TableAccount, Long>
{
    Optional<TableAccount> findByTableRestaurantTableIdAndStatusIn(
            Long tableId, Collection<AccountStatus> statuses);

    @Query("""
           SELECT a FROM TableAccount a
            WHERE (:status    IS NULL OR a.status = :status)
              AND (:tableId   IS NULL OR a.restaurantTableId = :tableId)
              AND (:waiterId IS NULL OR a.waiterId = :waiterId)
              AND (:from      IS NULL OR a.openedAt >= :from)
              AND (:to        IS NULL OR a.openedAt <= :to)
           """)
    Page<TableAccount> search(@Param("status")    AccountStatus status,
                              @Param("tableId")   Long tableId,
                              @Param("waiterId")  Long waiterId,
                              @Param("from")      LocalDateTime from,
                              @Param("to")        LocalDateTime to,
                              Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM TableAccount a WHERE a.tableAccountId = :tableAccountId")
    Optional<TableAccount> findByIdForUpdate(@Param("tableAccountId") Long tableAccountId);
}
