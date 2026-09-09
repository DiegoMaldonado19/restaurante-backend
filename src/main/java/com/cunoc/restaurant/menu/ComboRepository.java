package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.model.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long>
{
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndComboIdNot(String name, Long comboId);

    @Query("""
           SELECT c FROM Combo c
            WHERE (:active IS NULL OR c.active = :active)
            ORDER BY c.name ASC
           """)
    List<Combo> search(@Param("active") Boolean active);
}
