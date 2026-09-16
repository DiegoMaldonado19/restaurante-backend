package com.cunoc.restaurant.ordering.validation;

import com.cunoc.restaurant.ordering.dto.OrderLineDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExactlyOneProductValidatorTest
{
    private static Validator validator;

    @BeforeAll
    static void setUpValidator()
    {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void platilloSinComboEsValido()
    {
        assertThat(validator.validate(new OrderLineDTO(1L, null, 1, List.of(), null))).isEmpty();
    }

    @Test
    void comboSinPlatilloEsValido()
    {
        assertThat(validator.validate(new OrderLineDTO(null, 9L, 1, List.of(), "Para compartir"))).isEmpty();
    }

    @Test
    void ambosFallaEnDishId()
    {
        var violations = validator.validate(new OrderLineDTO(1L, 9L, 1, List.of(), null));
        assertThat(violations).anyMatch(v -> "dishId".equals(v.getPropertyPath().toString()));
    }

    @Test
    void ningunoFallaEnDishId()
    {
        var violations = validator.validate(new OrderLineDTO(null, null, 1, List.of(), null));
        assertThat(violations).anyMatch(v -> "dishId".equals(v.getPropertyPath().toString()));
    }
}
