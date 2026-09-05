package com.cunoc.restaurant.menu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Combo o promocion: varios platillos a un precio especial (comboPrice). Sus platillos van en combo_item. */
@Entity
@Table(name = "combo")
@Getter
@Setter
@NoArgsConstructor
public class Combo
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long comboId;

    @Column(length = 80, nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal comboPrice;

    @Column(nullable = false)
    private boolean active;
}
