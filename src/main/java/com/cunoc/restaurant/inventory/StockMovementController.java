package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.common.security.CurrentUser;
import com.cunoc.restaurant.inventory.dto.RegisterStockAdjustmentDTO;
import com.cunoc.restaurant.inventory.dto.RegisterStockEntryDTO;
import com.cunoc.restaurant.inventory.dto.RegisterStockWasteDTO;
import com.cunoc.restaurant.inventory.dto.StockMovementView;
import com.cunoc.restaurant.inventory.model.MovementType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Tres rutas de escritura y una de lectura. Entrada, merma y ajuste escriben la misma
 * tabla pero piden datos distintos y validan distinto, asi que son tres endpoints y no
 * un POST /stock-movements con discriminador. La lectura si es una sola: el kardex es uno.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Movimientos de stock", description = "Kardex, entradas de mercaderia, mermas y ajustes")
public class StockMovementController
{
    private final InventoryService inventoryService;

    @GetMapping("/stock-movements")
    @Operation(summary = "Kardex: entradas, ventas, mermas y ajustes con fecha y responsable",
               description = "El kardex de un insumo es este listado con supply_id, no una ruta anidada.")
    @ApiResponse(responseCode = "200", description = "Pagina de movimientos")
    public PagedModel<StockMovementView> findAll(
            @RequestParam(name = "supply_id",     required = false) Long         supplyId,
            @RequestParam(name = "movement_type", required = false) MovementType movementType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "user_id",       required = false) Long         userId,
            @ParameterObject                                        Pageable     pageable)
    {
        return new PagedModel<>(
                inventoryService.searchMovements(supplyId, movementType, from, to, userId, pageable));
    }

    @PostMapping("/stock-entries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Entrada de mercaderia: cantidad, fecha y costo de compra",
               description = "Sube el saldo y sobrescribe supply.unit_cost con el costo de compra.")
    @ApiResponse(responseCode = "201", description = "Entrada registrada")
    @ApiResponse(responseCode = "404", description = "SUPPLY_NOT_FOUND")
    public StockMovementView registerEntry(@Valid @RequestBody RegisterStockEntryDTO request)
    {
        return inventoryService.registerEntry(request, CurrentUser.id());
    }

    @PostMapping("/stock-wastes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Merma por vencimiento, dano o error de manejo")
    @ApiResponse(responseCode = "201", description = "Merma registrada")
    @ApiResponse(responseCode = "404", description = "SUPPLY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "WASTE_EXCEEDS_STOCK")
    public StockMovementView registerWaste(@Valid @RequestBody RegisterStockWasteDTO request)
    {
        return inventoryService.registerWaste(request, CurrentUser.id());
    }

    @PostMapping("/stock-adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajuste manual con motivo obligatorio, positivo o negativo")
    @ApiResponse(responseCode = "201", description = "Ajuste registrado")
    @ApiResponse(responseCode = "404", description = "SUPPLY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "INSUFFICIENT_STOCK")
    public StockMovementView registerAdjustment(@Valid @RequestBody RegisterStockAdjustmentDTO request)
    {
        return inventoryService.registerAdjustment(request, CurrentUser.id());
    }
}
