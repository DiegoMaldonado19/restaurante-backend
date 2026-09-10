package com.cunoc.restaurant.restaurant;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.restaurant.model.RestaurantTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long>
{
    boolean existsByTableNumber(int tableNumber);

    boolean existsByTableNumberAndRestaurantTableIdNot(int tableNumber, Long restaurantTableId);

    // Filtros de GET /tables: zone, status, min_capacity. Solo las mesas activas:
    // una mesa dada de baja por mantenimiento sale del salon y deja de ofrecerse.
    @Query("""
           SELECT t FROM RestaurantTable t
            WHERE t.active = true
              AND (:zone        IS NULL OR t.zone = :zone)
              AND (:status      IS NULL OR t.status = :status)
              AND (:minCapacity IS NULL OR t.capacity >= :minCapacity)
           """)
    Page<RestaurantTable> search(@Param("zone")        TableZone   zone,
                                 @Param("status")      TableStatus status,
                                 @Param("minCapacity") Integer     minCapacity,
                                 Pageable pageable);

    /**
     * Lectura para escribir el estado en transitionTo(). El bloqueo de fila hace la
     * transicion atomica: dos meseros pidiendo la cuenta de la misma mesa a la vez no
     * pueden pisarse la actualizacion (mismo patron que SupplyRepository.findByIdForUpdate).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RestaurantTable t WHERE t.restaurantTableId = :restaurantTableId")
    Optional<RestaurantTable> findByIdForUpdate(@Param("restaurantTableId") Long restaurantTableId);
}