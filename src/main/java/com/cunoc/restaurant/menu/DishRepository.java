package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.model.Dish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DishRepository extends JpaRepository<Dish, Long>
{
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndDishIdNot(String name, Long dishId);

    @Query("""
           SELECT d FROM Dish d
            WHERE (:categoryId IS NULL OR d.category.dishCategoryId = :categoryId)
              AND (:active     IS NULL OR d.active = :active)
              AND (:search     IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
           """)
    Page<Dish> search(@Param("categoryId") Long    categoryId,
                      @Param("search")     String  search,
                      @Param("active")     Boolean active,
                      Pageable pageable);
}
