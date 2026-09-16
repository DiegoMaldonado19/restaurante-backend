package com.cunoc.restaurant.ordering.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** BY_PERSON trae person_count y no items; BY_ITEM trae items y no person_count. */
@Documented
@Constraint(validatedBy = ConsistentSplitValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsistentSplit
{
    String message() default "La división por persona usa person_count; la división por ítem usa items";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
