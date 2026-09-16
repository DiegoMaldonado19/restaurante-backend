package com.cunoc.restaurant.ordering.validation;

import com.cunoc.restaurant.ordering.dto.SplitAccountDTO;
import com.cunoc.restaurant.ordering.model.SplitMode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsistentSplitValidator implements ConstraintValidator<ConsistentSplit, SplitAccountDTO>
{
    @Override
    public boolean isValid(SplitAccountDTO request, ConstraintValidatorContext context)
    {
        if (request == null || request.mode() == null)
            return true;

        if (request.mode() == SplitMode.BY_PERSON)
            return validPersonSplit(request, context);

        return validItemSplit(request, context);
    }

    private static boolean validPersonSplit(SplitAccountDTO request, ConstraintValidatorContext context)
    {
        var itemsOk = request.items() == null || request.items().isEmpty();
        if (request.personCount() != null && itemsOk)
            return true;

        String field = request.personCount() == null ? "personCount" : "items";
        String message = request.personCount() == null
                ? "Indique cuántas personas para la división"
                : "La división por persona no lleva líneas de ítems";
        return failOn(context, field, message);
    }

    private static boolean validItemSplit(SplitAccountDTO request, ConstraintValidatorContext context)
    {
        var itemsOk = request.items() != null && !request.items().isEmpty();
        if (request.personCount() == null && itemsOk)
            return true;

        String field = !itemsOk ? "items" : "personCount";
        String message = !itemsOk
                ? "Se requieren líneas de ítems para la división por ítem"
                : "La división por ítem no lleva número de personas";
        return failOn(context, field, message);
    }

    private static boolean failOn(ConstraintValidatorContext context, String field, String message)
    {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
