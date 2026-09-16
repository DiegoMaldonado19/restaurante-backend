package com.cunoc.restaurant.ordering.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Una línea de comanda trae dish_id o combo_id, nunca ambos ni ninguno. */
@Documented
@Constraint(validatedBy = ExactlyOneProductValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExactlyOneProduct
{
    String message() default "La línea debe traer un platillo o un combo, pero no ambos";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
