package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Long>
{
    /**
     * Lectura con bloqueo para incrementar el numero de factura de forma atomica:
     * dos cajeros facturando al mismo tiempo no pueden obtener el mismo invoiceNumber
     * (mismo patron que RestaurantTableRepository.findByIdForUpdate).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.sequenceId = :sequenceId")
    Optional<InvoiceSequence> findByIdForUpdate(@Param("sequenceId") Long sequenceId);
}
