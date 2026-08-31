package com.cunoc.restaurant.inventory.model;

/**
 * Lista cerrada del enunciado ("kg, litro, unidad, gramo"). Es enum y no tabla porque
 * la aritmetica de recetas depende de la unidad: una unidad nueva es codigo nuevo.
 */
public enum MeasureUnit
{
    KG,
    GRAM,
    LITER,
    MILLILITER,
    UNIT
}
