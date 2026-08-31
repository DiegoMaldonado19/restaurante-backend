package com.cunoc.restaurant.customer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** El telefono es la clave natural: es como el enunciado identifica al cliente y lo que el cajero teclea. */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @Column(length = 80, nullable = false)
    private String fullName;

    @Column(length = 20, nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
