package com.cunoc.restaurant.dining;

import com.cunoc.restaurant.dining.model.Reservation;
import com.cunoc.restaurant.dining.model.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>
{
    // Filtros de GET /reservations: date (dia completo), status, tableId. El filtro "search"
    // por nombre/telefono se resuelve en el Service via CustomerService, no aqui.
    @Query("""
           SELECT r FROM Reservation r
            WHERE (:from    IS NULL OR r.reservedAt >= :from)
              AND (:to      IS NULL OR r.reservedAt <  :to)
              AND (:status  IS NULL OR r.status = :status)
              AND (:tableId IS NULL OR r.restaurantTableId = :tableId)
           ORDER BY r.reservedAt ASC
           """)
    Page<Reservation> search(@Param("from")    LocalDateTime from,
                             @Param("to")      LocalDateTime to,
                             @Param("status")  ReservationStatus status,
                             @Param("tableId") Long tableId,
                             Pageable pageable);

    // Reservas activas (BOOKED) de una mesa que se solapan con la ventana [from, to).
    // Usado para bloquear el horario al crear (RESERVATION_SLOT_UNAVAILABLE) y para
    // decidir si sentar esta retownship esta dentro de su ventana (RESERVATION_NOT_DUE).
    @Query("""
           SELECT r FROM Reservation r
            WHERE r.restaurantTableId = :tableId
              AND r.status = com.cunoc.restaurant.dining.model.ReservationStatus.BOOKED
              AND r.reservedAt < :to
              AND r.reservedAt >= :from
           """)
    List<Reservation> findActiveOverlapping(@Param("tableId") Long tableId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.reservationId = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}
