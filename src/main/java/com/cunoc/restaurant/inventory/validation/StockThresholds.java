package com.cunoc.restaurant.inventory.validation;

import java.math.BigDecimal;

/** Lo que @ConsistentStockThresholds necesita leer. Lo implementan el alta y la edicion. */
public interface StockThresholds
{
    BigDecimal minStock();

    BigDecimal maxStock();
}
