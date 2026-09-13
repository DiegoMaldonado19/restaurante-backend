package com.cunoc.restaurant.report.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Regla de campo cruzado: se sabe con solo mirar el cuerpo, sin consultar la base. */
@Documented
@Constraint(validatedBy = ValidDateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange
{
    String message() default "La fecha inicial no puede ser posterior a la final";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
