package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.dto.CreateDishDTO;
import com.cunoc.restaurant.menu.dto.DishDetailView;
import com.cunoc.restaurant.menu.dto.DishView;
import com.cunoc.restaurant.menu.dto.UpdateDishAvailabilityDTO;
import com.cunoc.restaurant.menu.dto.UpdateDishDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/dishes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Platillos", description = "Catalogo del menu: alta, edicion, disponibilidad manual y baja")
public class DishController
{
    private final DishService dishService;
    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "Catalogo de platillos con sus filtros",
               description = "Filtros: category_id, search, active y available. available combina la "
                           + "bandera manual con el stock real y se aplica sobre la pagina.")
    @ApiResponse(responseCode = "200", description = "Pagina de platillos")
    public PagedModel<DishView> findAll(
            @RequestParam(name = "category_id", required = false) Long     categoryId,
            @RequestParam(                      required = false) String   search,
            @RequestParam(                      required = false) Boolean  active,
            @RequestParam(                      required = false) Boolean  available,
            @ParameterObject                                      Pageable pageable)
    {
        return new PagedModel<>(dishService.search(categoryId, search, active, available, pageable));
    }

    @PostMapping
    @Operation(summary = "Da de alta un platillo",
               description = "Nace disponible; su receta y su costo se definen aparte.")
    @ApiResponse(responseCode = "201", description = "Platillo creado")
    @ApiResponse(responseCode = "404", description = "DISH_CATEGORY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "DISH_NAME_TAKEN")
    public ResponseEntity<DishView> create(@Valid @RequestBody CreateDishDTO request)
    {
        var dish = dishService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/dishes/" + dish.dishId()))
                .body(dish);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del platillo con su receta vigente, costo de produccion y margen")
    @ApiResponse(responseCode = "200", description = "Ficha del platillo")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND")
    public DishDetailView findById(@PathVariable Long id)
    {
        return menuService.dishDetail(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica los datos del platillo",
               description = "No toca la disponibilidad manual: esa tiene su propio endpoint.")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND | DISH_CATEGORY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "DISH_NAME_TAKEN")
    public DishView update(@PathVariable Long id, @Valid @RequestBody UpdateDishDTO request)
    {
        return dishService.update(id, request);
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Marca el platillo disponible o no disponible a mano",
               description = "El administrador o cocina lo apagan. No afecta el calculo automatico por stock.")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND")
    public DishView changeAvailability(@PathVariable Long id,
                                       @Valid @RequestBody UpdateDishAvailabilityDTO request)
    {
        return dishService.changeAvailability(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Da de baja el platillo",
               description = "Baja logica: hay comandas y facturas historicas que lo referencian.")
    @ApiResponse(responseCode = "204", description = "Platillo dado de baja")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "DISH_IN_USE")
    public ResponseEntity<Void> delete(@PathVariable Long id)
    {
        dishService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
