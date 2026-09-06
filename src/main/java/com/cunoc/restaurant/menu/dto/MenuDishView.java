package com.cunoc.restaurant.menu.dto;

import java.math.BigDecimal;
import java.util.List;

/** Un platillo del menu operativo con sus modificadores y su tiempo estimado de preparacion. */
public record MenuDishView(
        Long               dishId,
        String             name,
        String             categoryName,
        BigDecimal         salePrice,
        int                prepMinutes,
        String             imageUrl,
        List<ModifierView> modifiers)
{ }
