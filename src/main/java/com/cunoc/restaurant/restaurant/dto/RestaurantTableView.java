package com.cunoc.restaurant.restaurant.dto;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.restaurant.model.RestaurantTable;

public record RestaurantTableView(
        Long        restaurantTableId,
        int         tableNumber,
        int         capacity,
        TableZone   zone,
        TableStatus status)
{
    public static RestaurantTableView from(RestaurantTable table)
    {
        return new RestaurantTableView(
                table.getRestaurantTableId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getZone(),
                table.getStatus());
    }
}