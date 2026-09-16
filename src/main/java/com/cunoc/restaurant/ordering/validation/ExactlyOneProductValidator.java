package com.cunoc.restaurant.ordering.validation;

import com.cunoc.restaurant.ordering.dto.OrderLineDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExactlyOneProductValidator implements ConstraintValidator<ExactlyOneProduct, OrderLineDTO>
{
    @Override
    public boolean isValid(OrderLineDTO line, ConstraintValidatorContext context)
    {
        if (line == null)
            return true;

        var hasDish = line.dishId() != null;
        var hasCombo = line.comboId() != null;
        if (hasDish ^ hasCombo)
            return true;

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("dishId")
                .addConstraintViolation();
        return false;
    }
}
