package com.cunoc.restaurant.ordering.validation;

import com.cunoc.restaurant.ordering.dto.SplitAccountDTO;
import com.cunoc.restaurant.ordering.dto.SplitLineDTO;
import com.cunoc.restaurant.ordering.model.SplitMode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistentSplitValidatorTest
{
    private static Validator validator;

    @BeforeAll
    static void setUpValidator()
    {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void byPersonConPersonCountYSinItemsEsValido()
    {
        assertThat(validator.validate(new SplitAccountDTO(SplitMode.BY_PERSON, 2, null))).isEmpty();
        assertThat(validator.validate(new SplitAccountDTO(SplitMode.BY_PERSON, 2, List.of()))).isEmpty();
    }

    @Test
    void byItemConLineasYSinPersonCountEsValido()
    {
        var dto = new SplitAccountDTO(SplitMode.BY_ITEM, null,
                List.of(new SplitLineDTO(148L, 1L)));
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void byPersonSinPersonCountFallaEnEseCampo()
    {
        var violations = validator.validate(new SplitAccountDTO(SplitMode.BY_PERSON, null, null));
        assertThat(violations).anyMatch(v -> "personCount".equals(v.getPropertyPath().toString()));
    }

    @Test
    void byItemSinLineasFallaEnItems()
    {
        var violations = validator.validate(new SplitAccountDTO(SplitMode.BY_ITEM, null, List.of()));
        assertThat(violations).anyMatch(v -> "items".equals(v.getPropertyPath().toString()));
    }
}
