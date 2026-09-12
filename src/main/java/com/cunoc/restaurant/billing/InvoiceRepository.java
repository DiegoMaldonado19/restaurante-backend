package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>
{
    Optional<Invoice> findByTableAccountId(Long tableAccountId);

    Optional<Invoice> findByAccountSplitId(Long accountSplitId);

    // Filtros del historial de facturas: waiterId, customerId, y rango de fechas de emision.
    @Query("""
           SELECT i FROM Invoice i
            WHERE (:waiterId   IS NULL OR i.waiterId = :waiterId)
              AND (:customerId IS NULL OR i.customerId = :customerId)
              AND (:from       IS NULL OR i.issuedAt >= :from)
              AND (:to         IS NULL OR i.issuedAt <= :to)
           ORDER BY i.issuedAt DESC
           """)
    Page<Invoice> search(@Param("waiterId")   Long waiterId,
                         @Param("customerId") Long customerId,
                         @Param("from")       LocalDateTime from,
                         @Param("to")         LocalDateTime to,
                         Pageable pageable);
}
