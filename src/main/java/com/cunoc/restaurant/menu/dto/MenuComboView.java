package com.cunoc.restaurant.menu.dto;

import java.math.BigDecimal;
import java.util.List;

/** Un combo activo del menu operativo con los platillos que incluye. */
public record MenuComboView(
        Long                comboId,
        String              name,
        String              description,
        BigDecimal          comboPrice,
        List<ComboItemView> items)
{ }
