package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.inventory.model.MovementType;
import com.cunoc.restaurant.inventory.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>
{
    List<StockMovement> findTop10BySupplySupplyIdOrderByCreatedAtDesc(Long supplyId);

    /** Los movimientos de un item de comanda, para poder devolver lo que consumio. */
    List<StockMovement> findByOrderItemId(Long orderItemId);

    // El kardex es una sola lectura con cinco filtros opcionales, todos por query param.
    @Query("""
           SELECT m FROM StockMovement m
            WHERE (:supplyId     IS NULL OR m.supply.supplyId = :supplyId)
              AND (:movementType IS NULL OR m.movementType    = :movementType)
              AND (:userId       IS NULL OR m.userId          = :userId)
              AND (:from         IS NULL OR m.createdAt      >= :from)
              AND (:to           IS NULL OR m.createdAt      <= :to)
           """)
    Page<StockMovement> search(@Param("supplyId")     Long          supplyId,
                               @Param("movementType") MovementType  movementType,
                               @Param("from")         LocalDateTime from,
                               @Param("to")           LocalDateTime to,
                               @Param("userId")       Long          userId,
                               Pageable pageable);
}
