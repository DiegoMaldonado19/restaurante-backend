package com.cunoc.restaurant.menu.dto;

import java.util.List;

/**
 * El menu operativo en una sola respuesta (GET /menu): los platillos disponibles con sus
 * modificadores y los combos activos. Es la primera llamada de la app de operacion al abrir
 * una mesa; existe para evitar N+1 peticiones desde la tableta.
 */
public record MenuView(
        List<MenuDishView>  dishes,
        List<MenuComboView> combos)
{ }
