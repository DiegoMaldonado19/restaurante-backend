package com.cunoc.restaurant.restaurant;

import com.cunoc.restaurant.restaurant.model.RestaurantSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/** Una sola fila (setting_id = 1): no hay consultas derivadas que declarar. */
public interface RestaurantSettingRepository extends JpaRepository<RestaurantSetting, Long>
{
}