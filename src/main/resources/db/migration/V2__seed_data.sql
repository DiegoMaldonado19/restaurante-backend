-- Datos minimos para que el sistema arranque: las dos tablas de catalogo, la fila
-- unica de configuracion y el administrador inicial.
-- Idempotente: se puede reaplicar sobre una base que ya los tenga.

-- =============================================================================
-- CATALOGOS
-- =============================================================================
-- El enunciado deja las dos listas abiertas ("proteina, verdura, lacteo, abarrote,
-- etc."), por eso son tablas y no enums: el administrador las crece desde la interfaz.

INSERT INTO supply_category (name) VALUES
    ('Proteina'),
    ('Verdura'),
    ('Lacteo'),
    ('Abarrote'),
    ('Bebida')
ON DUPLICATE KEY UPDATE active = TRUE;

INSERT INTO dish_category (name, display_order) VALUES
    ('Entrada',      1),
    ('Plato fuerte', 2),
    ('Bebida',       3),
    ('Postre',       4)
ON DUPLICATE KEY UPDATE active = TRUE;

-- =============================================================================
-- CONFIGURACION DEL RESTAURANTE
-- =============================================================================
-- Una sola fila (ck_restaurant_setting_single). Los valores iniciales son los
-- mismos de application.properties; a partir de aqui manda esta tabla, porque el
-- enunciado pide que impuestos, propina y programa de puntos sean configurables.

INSERT INTO restaurant_setting (setting_id, tax_percent, tip_suggested_percent,
                                points_per_currency_unit, currency_per_point)
VALUES (1, 12.00, 10.00, 1.0000, 0.1000)
ON DUPLICATE KEY UPDATE setting_id = setting_id;

-- =============================================================================
-- ADMINISTRADOR INICIAL
-- =============================================================================
-- Sin este usuario no hay con que iniciar sesion y no se puede dar de alta al resto
-- del personal. La contrasena es 'Admin123!' y el hash es BCrypt de coste 10,
-- generado aparte: nunca una contrasena en claro en una migracion.
-- CAMBIARLA DESDE PUT /api/v1/users/me/password DESPUES DEL PRIMER DESPLIEGUE.

INSERT INTO app_user (full_name, username, password_hash, role, status)
VALUES ('Administrador', 'admin',
        '$2a$10$ZmYibzb./fCq10qbi8j6k.INUwB4ROTU.Twp8iBPV96LA/hIb3u6.',
        'ADMIN', 'ACTIVE')
ON DUPLICATE KEY UPDATE status = 'ACTIVE';
