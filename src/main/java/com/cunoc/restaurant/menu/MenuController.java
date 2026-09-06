package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.dto.MenuView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@Validated
@Tag(name = "Menu operativo", description = "El menu que consume la app de operacion en una sola respuesta")
public class MenuController
{
    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "Menu operativo: platillos disponibles con sus modificadores y los combos activos",
               description = "Primera llamada de la app de operacion al abrir una mesa; evita N+1 desde la tableta.")
    @ApiResponse(responseCode = "200", description = "Menu operativo")
    public MenuView operationalMenu()
    {
        return menuService.operationalMenu();
    }
}
