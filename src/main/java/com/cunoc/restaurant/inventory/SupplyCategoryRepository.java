package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.inventory.model.SupplyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplyCategoryRepository extends JpaRepository<SupplyCategory, Long>
{
    List<SupplyCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
