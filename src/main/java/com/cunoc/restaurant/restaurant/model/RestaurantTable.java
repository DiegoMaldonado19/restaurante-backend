package com.cunoc.restaurant.restaurant.model;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * La mesa del salon. Su estado solo lo escribe RestaurantTableService.transitionTo(), que
 * valida la matriz de transiciones; el status nunca se toca por fuera. El numero es unico
 * (uq_restaurant_table_number) y active=false es la baja logica por mantenimiento.
 */
@Entity
@Table(name = "restaurant_table")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantTable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantTableId;

    @Column(nullable = false, unique = true)
    private int tableNumber;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TableZone zone;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TableStatus status;

    @Column(nullable = false)
    private boolean active;
}