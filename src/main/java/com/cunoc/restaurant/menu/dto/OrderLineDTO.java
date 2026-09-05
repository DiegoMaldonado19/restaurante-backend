package com.cunoc.restaurant.menu.dto;

import java.util.List;

/**
 * Una linea de comanda vista por el menu: un platillo, su cantidad y los modificadores
 * aplicados. Es el contrato de entrada de MenuService.explodeRecipe(): ordering lo
 * construye y menu lo traduce a la lista de insumos a descontar (SupplyConsumption).
 *
 * Vive en menu porque menu es el modulo consumido; ordering lo importa desde aqui, igual
 * que menu importa SupplyConsumption desde inventory. Asi la dependencia queda en una sola
 * direccion (menu -> ordering) y no se invierte.
 *
 * La firma la fija menu (Diego Avila) y ordering (Alexander) programa contra ella. Mientras
 * no cambie, ordering puede escribirse aunque el cuerpo de explodeRecipe() aun no exista.
 */
public record OrderLineDTO(Long dishId, int quantity, List<Long> modifierIds)
{ }
