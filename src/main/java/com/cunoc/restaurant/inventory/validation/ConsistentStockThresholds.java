package com.cunoc.restaurant.inventory.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Regla de campo cruzado: se sabe con solo mirar el cuerpo, sin consultar la base. */
@Documented
@Constraint(validatedBy = ConsistentStockThresholdsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsistentStockThresholds
{
    String message() default "El stock maximo no puede ser menor que el minimo";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
