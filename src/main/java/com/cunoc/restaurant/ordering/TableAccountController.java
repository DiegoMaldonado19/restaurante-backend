package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.ordering.dto.*;
import com.cunoc.restaurant.ordering.model.AccountStatus;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cuentas de mesa", description = "Gestión de cuentas de mesa: apertura, transferencia, fusión, división, cancelación y facturación.")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Validated
public class TableAccountController
{
    private final TableAccountService accountService;

    @GetMapping
    @Operation(summary = "Listar cuentas de mesa", description = "Filtra cuentas por estado, mesa, mesero y rango de fechas.")
    @ApiResponse(responseCode = "200", description = "Lista paginada de cuentas")
    public PagedModel<TableAccountView> list(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) Long waiterId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Pageable pageable)
    {
        return new PagedModel<>(accountService.search(status, tableId, waiterId, null, null, pageable));
    }

    @PostMapping
    @Operation(summary = "Abrir cuenta de mesa", description = "Abre una nueva cuenta en una mesa libre asignada a un mesero.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "404", description = "Mesa o cuenta no encontrada")
    @ApiResponse(responseCode = "409", description = "Mesa ocupada, reservada, capacidad excedida o cuenta ya abierta")
    public ResponseEntity<TableAccountView> open(@Valid @RequestBody OpenAccountDTO request)
    {
        TableAccountView view = accountService.open(request);
        return ResponseEntity.status(201).body(view);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Obtener cuenta de mesa", description = "Obtiene los detalles de una cuenta específica.")
    @ApiResponse(responseCode = "200", description = "Cuenta encontrada")
    @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    public TableAccountView getById(@PathVariable Long accountId)
    {
        return accountService.findById(accountId);
    }

    @PostMapping("/{accountId}/transfers")
    @Operation(summary = "Transferir cuenta a otra mesa", description = "Mueve la cuenta de una mesa a otra mesa libre.")
    @ApiResponse(responseCode = "200", description = "Cuenta transferida")
    @ApiResponse(responseCode = "409", description = "Mesa destino no disponible o reservada")
    public ResponseEntity<TableAccountView> transfer(@PathVariable Long accountId,
                                                     @Valid @RequestBody TransferAccountDTO request)
    {
        TableAccountView view = accountService.transfer(accountId, request);
        return ResponseEntity.ok(view);
    }

    @PostMapping("/{accountId}/merges")
    @Operation(summary = "Fusionar cuentas", description = "Fusiona una cuenta origen en la cuenta destino.")
    @ApiResponse(responseCode = "200", description = "Cuentas fusionadas")
    @ApiResponse(responseCode = "409", description = "Error al fusionar cuentas")
    public ResponseEntity<TableAccountView> merge(@PathVariable Long accountId,
                                                  @Valid @RequestBody MergeAccountDTO request)
    {
        TableAccountView view = accountService.merge(accountId, request);
        return ResponseEntity.ok(view);
    }

    @PostMapping("/{accountId}/splits")
    @Operation(summary = "Dividir cuenta", description = "Divide la cuenta por personas o por ítems.")
    @ApiResponse(responseCode = "200", description = "División creada")
    @ApiResponse(responseCode = "409", description = "División inválida o ítems ya asignados")
    public ResponseEntity<List<AccountSplitView>> split(@PathVariable Long accountId,
                                                        @Valid @RequestBody SplitAccountDTO request)
    {
        List<AccountSplitView> splits = accountService.split(accountId, request);
        return ResponseEntity.ok(splits);
    }

    @PatchMapping("/{accountId}/status")
    @Operation(summary = "Actualizar estado de cuenta", description = "Cambia el estado de la cuenta (ej. pedir factura).")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    public ResponseEntity<TableAccountView> updateStatus(@PathVariable Long accountId,
                                                         @Valid @RequestBody UpdateAccountStatusDTO request)
    {
        // Actualmente solo se soporta solicitar factura (OPEN -> BILL_REQUESTED)
        if (request.status() == AccountStatus.BILL_REQUESTED)
        {
            TableAccountView view = accountService.requestBill(accountId);
            return ResponseEntity.ok(view);
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Estado no soportado para actualización: " + request.status());
    }

    @PostMapping("/{accountId}/cancellations")
    @Operation(summary = "Cancelar cuenta", description = "Cancela una cuenta abierta o lista para cobro con motivo.")
    @ApiResponse(responseCode = "200", description = "Cuenta cancelada")
    @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    @ApiResponse(responseCode = "409", description = "Cuenta no se puede cancelar")
    public ResponseEntity<TableAccountView> cancel(@PathVariable Long accountId,
                                                   @Valid @RequestBody CancelAccountDTO request)
    {
        TableAccountView view = accountService.cancel(accountId, request);
        return ResponseEntity.ok(view);
    }
}
