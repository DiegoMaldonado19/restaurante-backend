package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.inventory.model.Supply;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplyRepository extends JpaRepository<Supply, Long>
{
    /**
     * Lectura para escribir el saldo. El bloqueo de fila es lo que impide que dos comandas
     * simultaneas sobre el mismo insumo pierdan una de las dos actualizaciones.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Supply s WHERE s.supplyId = :supplyId")
    Optional<Supply> findByIdForUpdate(@Param("supplyId") Long supplyId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndSupplyIdNot(String name, Long supplyId);

    // low_stock a FALSE no filtra: la interfaz manda la casilla solo cuando esta marcada.
    @Query("""
           SELECT s FROM Supply s
            WHERE (:categoryId IS NULL OR s.category.supplyCategoryId = :categoryId)
              AND (:active     IS NULL OR s.active = :active)
              AND (:lowStock   IS NULL OR :lowStock = FALSE OR s.currentStock <= s.minStock)
              AND (:search     IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
           """)
    Page<Supply> search(@Param("categoryId") Long    categoryId,
                        @Param("search")     String  search,
                        @Param("active")     Boolean active,
                        @Param("lowStock")   Boolean lowStock,
                        Pageable pageable);
}
