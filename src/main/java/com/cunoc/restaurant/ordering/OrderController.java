package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.ordering.dto.*;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
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

@Tag(name = "Comandas", description = "Gestión de comandas: envío, estados, cancelación y cola de cocina.")
@RestController
@RequiredArgsConstructor
@Validated
public class OrderController
{
    private final OrderService orderService;
    private final TableAccountService accountService;

    @GetMapping("/api/v1/orders")
    @Operation(summary = "Cola de comandas", description = "Lista ítems de comanda con filtros de estado, mesa y mesero.")
    @ApiResponse(responseCode = "200", description = "Lista paginada de ítems")
    public PagedModel<OrderItemView> searchQueue(
            @RequestParam(required = false) OrderItemStatus status,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) Long waiterId,
            @RequestParam(required = false) Boolean overdue,
            Pageable pageable)
    {
        return new PagedModel<>(orderService.searchQueue(status, tableId, waiterId, overdue, pageable));
    }

    @PostMapping("/api/v1/accounts/{accountId}/orders")
    @Operation(summary = "Enviar comanda", description = "Envía una ronda de comanda a la cocina para una cuenta.")
    @ApiResponse(responseCode = "201", description = "Comanda enviada")
    @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    @ApiResponse(responseCode = "409", description = "Cuenta no está abierta o stock insuficiente")
    public ResponseEntity<OrderTicketView> submit(@PathVariable Long accountId,
                                                  @Valid @RequestBody SubmitOrderDTO request)
    {
        OrderTicketView view = orderService.submit(accountId, request);
        return ResponseEntity.status(201).body(view);
    }

    @GetMapping("/api/v1/orders/{ticketId}")
    @Operation(summary = "Detalle de comanda", description = "Obtiene el detalle de una comanda con sus ítems.")
    @ApiResponse(responseCode = "200", description = "Comanda encontrada")
    @ApiResponse(responseCode = "404", description = "Comanda no encontrada")
    public OrderTicketView findTicket(@PathVariable Long ticketId)
    {
        return orderService.findTicket(ticketId);
    }

    @PatchMapping("/api/v1/order-items/{orderItemId}/status")
    @Operation(summary = "Actualizar estado de ítem", description = "Cambia el estado de un ítem de comanda (cocina/mesero).")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "404", description = "Ítem no encontrado")
    @ApiResponse(responseCode = "409", description = "Transición de estado no válida")
    public OrderItemView updateStatus(@PathVariable Long orderItemId,
                                      @Valid @RequestBody UpdateOrderItemStatusDTO request)
    {
        return orderService.updateStatus(orderItemId, request);
    }

    @PutMapping("/api/v1/order-items/{orderItemId}")
    @Operation(summary = "Actualizar ítem", description = "Corrige cantidad, modificadores o nota de un ítem (solo si está RECIBIDO).")
    @ApiResponse(responseCode = "200", description = "Ítem actualizado")
    @ApiResponse(responseCode = "404", description = "Ítem no encontrado")
    @ApiResponse(responseCode = "409", description = "Ítem en preparación, no se puede editar")
    public OrderItemView update(@PathVariable Long orderItemId,
                                @Valid @RequestBody UpdateOrderItemDTO request)
    {
        return orderService.update(orderItemId, request);
    }

    @DeleteMapping("/api/v1/order-items/{orderItemId}")
    @Operation(summary = "Eliminar ítem", description = "Elimina un ítem y devuelve stock (solo si está RECIBIDO).")
    @ApiResponse(responseCode = "204", description = "Ítem eliminado")
    @ApiResponse(responseCode = "404", description = "Ítem no encontrado")
    @ApiResponse(responseCode = "409", description = "Ítem en preparación, no se puede eliminar")
    public ResponseEntity<Void> delete(@PathVariable Long orderItemId)
    {
        orderService.delete(orderItemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/order-items/{orderItemId}/unavailabilities")
    @Operation(summary = "Marcar no disponible", description = "Marca un ítem como no disponible y devuelve stock (solo cocina).")
    @ApiResponse(responseCode = "204", description = "Ítem marcado como no disponible")
    @ApiResponse(responseCode = "404", description = "Ítem no encontrado")
    @ApiResponse(responseCode = "409", description = "Ítem en preparación, no se puede marcar")
    public ResponseEntity<Void> markUnavailable(@PathVariable Long orderItemId)
    {
        orderService.markUnavailable(orderItemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/order-items/{orderItemId}/cancellations")
    @Operation(summary = "Cancelar ítem", description = "Cancela un ítem de la comanda (solo ADMIN). No devuelve stock.")
    @ApiResponse(responseCode = "204", description = "Ítem cancelado")
    @ApiResponse(responseCode = "404", description = "Ítem no encontrado")
    public ResponseEntity<Void> cancel(@PathVariable Long orderItemId,
                                       @Valid @RequestBody CancelOrderItemDTO request)
    {
        orderService.cancel(orderItemId, request);
        return ResponseEntity.noContent().build();
    }
}
