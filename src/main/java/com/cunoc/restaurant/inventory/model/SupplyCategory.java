package com.cunoc.restaurant.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Es tabla y no enum porque el enunciado deja la lista abierta con un "etc.". */
@Entity
@Table(name = "supply_category")
@Getter
@Setter
@NoArgsConstructor
public class SupplyCategory
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplyCategoryId;

    @Column(length = 60, nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean active;
}
