package com.cunoc.restaurant.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** El cuerpo de PUT /recipe: la lista de insumos con sus cantidades. Reemplaza la receta vigente. */
public record RecipeDTO(
        @NotEmpty(message = "La receta debe incluir al menos un insumo")
        @Valid
        List<RecipeItemDTO> items)
{ }
