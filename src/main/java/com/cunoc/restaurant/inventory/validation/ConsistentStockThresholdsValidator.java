package com.cunoc.restaurant.inventory.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsistentStockThresholdsValidator
        implements ConstraintValidator<ConsistentStockThresholds, StockThresholds>
{
    /** El maximo es opcional; solo hay algo que comparar cuando vienen los dos. */
    @Override
    public boolean isValid(StockThresholds thresholds, ConstraintValidatorContext context)
    {
        if (thresholds == null || thresholds.minStock() == null || thresholds.maxStock() == null)
        {
            return true;
        }

        return thresholds.maxStock().compareTo(thresholds.minStock()) >= 0;
    }
}
