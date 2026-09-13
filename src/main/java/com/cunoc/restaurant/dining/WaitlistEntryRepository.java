package com.cunoc.restaurant.dining;

import com.cunoc.restaurant.dining.model.WaitlistEntry;
import com.cunoc.restaurant.dining.model.WaitlistStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long>
{
    // Cola en orden de llegada. fitsCapacity filtra solo quienes caben en la mesa que
    // se libero (la sugerencia automatica del enunciado); null trae la cola completa.
    @Query("""
           SELECT w FROM WaitlistEntry w
            WHERE (:status       IS NULL OR w.status = :status)
              AND (:fitsCapacity IS NULL OR w.guestCount <= :fitsCapacity)
           ORDER BY w.arrivedAt ASC
           """)
    List<WaitlistEntry> search(@Param("status") WaitlistStatus status,
                               @Param("fitsCapacity") Integer fitsCapacity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WaitlistEntry w WHERE w.waitlistEntryId = :id")
    Optional<WaitlistEntry> findByIdForUpdate(@Param("id") Long id);
}
