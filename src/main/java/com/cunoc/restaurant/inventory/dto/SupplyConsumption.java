package com.cunoc.restaurant.inventory.dto;

import java.math.BigDecimal;

/**
 * Una linea de insumo consumido por una venta. Es el contrato entre menu e inventory:
 * menu.explodeRecipe() lo devuelve e inventory.registerSaleConsumption() lo recibe.
 * Vive aqui porque es el parametro del metodo de inventory, y menu ya depende de inventory.
 */
public record SupplyConsumption(Long supplyId, BigDecimal quantity)
{ }
