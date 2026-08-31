package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.inventory.dto.CreateSupplyDTO;
import com.cunoc.restaurant.inventory.dto.SupplyDetailView;
import com.cunoc.restaurant.inventory.dto.SupplyView;
import com.cunoc.restaurant.inventory.dto.UpdateSupplyDTO;
import com.cunoc.restaurant.inventory.dto.UpdateSupplyStatusDTO;
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
@RequestMapping("/api/v1/supplies")
@RequiredArgsConstructor
@Validated
@Tag(name = "Insumos", description = "Catalogo de materia prima con su saldo y sus umbrales")
// OJO: spring.jackson.property-naming-strategy=SNAKE_CASE solo aplica al cuerpo JSON.
// Un query param de dos palabras necesita @RequestParam(name = "...") explicito o llega
// siempre nulo, y un filtro que no filtra no falla: devuelve todo.
public class SupplyController
{
    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Catalogo de insumos con su saldo",
               description = "low_stock=true deja solo los que llegaron al minimo: es la alerta de "
                           + "reabastecimiento del enunciado.")
    @ApiResponse(responseCode = "200", description = "Pagina de insumos")
    public PagedModel<SupplyView> findAll(
            @RequestParam(name = "category_id", required = false) Long     categoryId,
            @RequestParam(                      required = false) String   search,
            @RequestParam(                      required = false) Boolean  active,
            @RequestParam(name = "low_stock",   required = false) Boolean  lowStock,
            @ParameterObject                                      Pageable pageable)
    {
        return new PagedModel<>(inventoryService.search(categoryId, search, active, lowStock, pageable));
    }

    @PostMapping
    @Operation(summary = "Da de alta un insumo",
               description = "Nace con saldo cero: el saldo solo se mueve con un movimiento de stock.")
    @ApiResponse(responseCode = "201", description = "Insumo creado")
    @ApiResponse(responseCode = "404", description = "SUPPLY_CATEGORY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SUPPLY_NAME_TAKEN")
    public ResponseEntity<SupplyView> create(@Valid @RequestBody CreateSupplyDTO request)
    {
        var supply = inventoryService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/supplies/" + supply.supplyId()))
                .body(supply);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha del insumo con su valor en existencia y sus ultimos movimientos")
    @ApiResponse(responseCode = "404", description = "SUPPLY_NOT_FOUND")
    public SupplyDetailView findById(@PathVariable Long id)
    {
        return inventoryService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica nombre, unidad, categoria y umbrales",
               description = "No toca el saldo ni el costo unitario: el saldo lo mueve un movimiento "
                           + "y el costo lo fija una entrada de mercaderia.")
    @ApiResponse(responseCode = "404", description = "SUPPLY_NOT_FOUND · SUPPLY_CATEGORY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SUPPLY_NAME_TAKEN")
    public SupplyView update(@PathVariable Long id, @Valid @RequestBody UpdateSupplyDTO request)
    {
        return inventoryService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activa o desactiva el insumo",
               description = "Un insumo inactivo deja de tener saldo utilizable, con lo que los "
                           + "platillos de cuya receta forma parte quedan no disponibles solos.")
    @ApiResponse(responseCode = "404", description = "SUPPLY_NOT_FOUND")
    public SupplyView changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateSupplyStatusDTO request)
    {
        return inventoryService.changeStatus(id, request);
    }
}
