package com.cunoc.restaurant.restaurant;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.restaurant.dto.CreateTableDTO;
import com.cunoc.restaurant.restaurant.dto.RestaurantTableView;
import com.cunoc.restaurant.restaurant.dto.UpdateTableDTO;
import com.cunoc.restaurant.restaurant.dto.UpdateTableStatusDTO;
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
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
@Validated
@Tag(name = "Mesas", description = "Mesas del salon con su estado y su maquina de transiciones")
public class TableController
{
    private final RestaurantTableService tableService;

    @GetMapping
    @Operation(summary = "Mesas activas con su estado actual",
               description = "El panel de ocupacion y el salon la consultan. Los filtros dejan "
                           + "ver solo una zona, solo un estado o mesas que alcancen una capacidad.")
    @ApiResponse(responseCode = "200", description = "Pagina de mesas")
    public PagedModel<RestaurantTableView> findAll(
            @RequestParam(required = false) TableZone   zone,
            @RequestParam(required = false) TableStatus status,
            @RequestParam(name = "min_capacity", required = false) Integer minCapacity,
            @ParameterObject Pageable pageable)
    {
        return new PagedModel<>(tableService.search(zone, status, minCapacity, pageable));
    }

    @PostMapping
    @Operation(summary = "Da de alta una mesa",
               description = "El estado inicial casi siempre es FREE; RESERVED queda para quien "
                           + "crea la mesa con una reserva ya asignada.")
    @ApiResponse(responseCode = "201", description = "Mesa creada")
    @ApiResponse(responseCode = "409", description = "TABLE_NUMBER_TAKEN")
    public ResponseEntity<RestaurantTableView> create(@Valid @RequestBody CreateTableDTO request)
    {
        var table = tableService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/tables/" + table.restaurantTableId()))
                .body(table);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha de la mesa")
    @ApiResponse(responseCode = "404", description = "TABLE_NOT_FOUND")
    public RestaurantTableView findById(@PathVariable Long id)
    {
        return tableService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica numero, capacidad y zona",
               description = "No toca el estado: el estado solo se mueve por la maquina de transiciones.")
    @ApiResponse(responseCode = "404", description = "TABLE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TABLE_NUMBER_TAKEN")
    public RestaurantTableView update(@PathVariable Long id, @Valid @RequestBody UpdateTableDTO request)
    {
        return tableService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transicion manual del estado",
               description = "La valvula de recuperacion cuando una mesa queda mal. Valida la misma "
                           + "matriz que usan dining, ordering y billing, y lanza 409 si el salto no "
                           + "esta permitido.")
    @ApiResponse(responseCode = "404", description = "TABLE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "INVALID_TABLE_TRANSITION")
    public RestaurantTableView changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody UpdateTableStatusDTO request)
    {
        return tableService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Da de baja la mesa por mantenimiento",
               description = "Baja logica: active = false. Solo se permite en mesa libre; una mesa "
                           + "con cuenta abierta o por cobrar responde 409 TABLE_NOT_FREE.")
    @ApiResponse(responseCode = "204", description = "Mesa dada de baja")
    @ApiResponse(responseCode = "404", description = "TABLE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TABLE_NOT_FREE")
    public ResponseEntity<Void> deactivate(@PathVariable Long id)
    {
        tableService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}