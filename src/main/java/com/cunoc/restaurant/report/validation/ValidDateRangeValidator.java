package com.cunoc.restaurant.report.validation;

import com.cunoc.restaurant.report.dto.ReportRange;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, ReportRange>
{
    /**
     * La violacion se cuelga de "to" y no de la clase: una restriccion de nivel TYPE
     * produce un error global, y el GlobalExceptionHandler solo lee getFieldErrors(),
     * asi que el 400 saldria con la lista fields[] vacia.
     */
    @Override
    public boolean isValid(ReportRange range, ConstraintValidatorContext context)
    {
        if (range == null || range.from() == null || range.to() == null)
        {
            return true;
        }

        if (!range.from().isAfter(range.to()))
        {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
               .addPropertyNode("to")
               .addConstraintViolation();

        return false;
    }
}
