package com.cunoc.restaurant.restaurant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * La unica fila de configuracion editable del sistema (ck_restaurant_setting_single exige
 * setting_id = 1 y V2 siembra la fila). Impuesto, propina sugerida y las dos tasas del
 * programa de puntos. No es una tabla clave-valor: son cuatro columnas tipadas. Por eso
 * settingId no es IDENTITY: la fila ya existe y no se crea nunca mas.
 */
@Entity
@Table(name = "restaurant_setting")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantSetting
{
    @Id
    private Long settingId = 1L;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal taxPercent;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal tipSuggestedPercent;

    @Column(precision = 12, scale = 4, nullable = false)
    private BigDecimal pointsPerCurrencyUnit;

    @Column(precision = 12, scale = 4, nullable = false)
    private BigDecimal currencyPerPoint;
}