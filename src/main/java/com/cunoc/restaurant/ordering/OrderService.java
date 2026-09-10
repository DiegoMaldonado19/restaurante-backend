package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.common.security.CurrentUser;
import com.cunoc.restaurant.inventory.InventoryService;
import com.cunoc.restaurant.inventory.dto.SupplyConsumption;
import com.cunoc.restaurant.menu.MenuService;
import com.cunoc.restaurant.ordering.dto.*;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.OrderItemModifier;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.ordering.model.OrderTicket;
import com.cunoc.restaurant.ordering.model.TableAccount;
import com.cunoc.restaurant.restaurant.RestaurantTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de comandas: enviar rondas, ciclo de vida de ítems, cola de cocina.
 * La transacción más delicada del sistema: submit() descontar stock en la misma transacción.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService
{
    private final TableAccountRepository accountRepository;
    private final OrderTicketRepository ticketRepository;
    private final OrderItemRepository itemRepository;
    private final OrderItemModifierRepository modifierRepository;
    private final MenuService menuService;
    private final InventoryService inventoryService;
    private final RestaurantTableService tableService;

    // --- Máquina de estados del ítem ----------------------------------------

    private static final Map<OrderItemStatus, Set<OrderItemStatus>> ITEM_TRANSITIONS = Map.of(
            OrderItemStatus.RECEIVED,        Set.of(OrderItemStatus.IN_PREPARATION),
            OrderItemStatus.IN_PREPARATION,  Set.of(OrderItemStatus.READY),
            OrderItemStatus.READY,           Set.of(OrderItemStatus.DELIVERED));

    // --- Enviar una ronda (la transacción completa) -------------------------

    /**
     * Envía una ronda de comanda a la cocina. Todo en una sola transacción;
     * si algo falla, no se inserta nada.
     */
    @Transactional
    public OrderTicketView submit(Long accountId, SubmitOrderDTO request)
    {
        var account = accountForUpdate(accountId);

        if (account.getStatus() != AccountStatus.OPEN && account.getStatus() != AccountStatus.BILL_REQUESTED)
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_OPEN,
                    "No se puede enviar comanda a una cuenta que no está abierta o lista para cobro. Estado: " + account.getStatus() + ".");

        // Si la mesa pidió la cuenta y llega una ronda más, vuelve a OCCUPIED
        if (account.getStatus() == AccountStatus.BILL_REQUESTED)
        {
            tableService.transitionTo(account.getRestaurantTableId(), TableStatus.OCCUPIED);
            account.setStatus(AccountStatus.OPEN);
        }

        var ticket = new OrderTicket();
        ticket.setAccount(account);
        ticket.setWaiterId(account.getWaiterId());
        ticket.setSubmittedAt(LocalDateTime.now());

        // Convertir OrderLineDTO de ordering a OrderLineDTO de menu para explodeRecipe
        var menuLines = request.items().stream()
                .map(line -> new com.cunoc.restaurant.menu.dto.OrderLineDTO(
                        line.dishId(), line.quantity(), line.modifierIds()))
                .collect(Collectors.toList());

        // Descontar stock de todos los ítems primero (si falla, nada se inserta)
        var consumption = menuService.explodeRecipe(menuLines);
        inventoryService.registerSaleConsumption(consumption, 0L, CurrentUser.id());

        // Crear los ítems de la comanda
        for (var line : request.items())
        {
            var item = new OrderItem();
            item.setTicket(ticket);
            item.setDishId(line.dishId());
            item.setQuantity(line.quantity());
            item.setUnitPrice(dishSalePrice(line.dishId()));
            item.setUnitCost(menuService.productionCost(line.dishId()));
            item.setNote(null);
            item.setStatus(OrderItemStatus.RECEIVED);
            item.setSubmittedAt(LocalDateTime.now());

            itemRepository.save(item);

            // Guardar modificadores
            if (line.modifierIds() != null)
            {
                for (var modifierId : line.modifierIds())
                {
                    var modifier = new OrderItemModifier();
                    modifier.setOrderItem(item);
                    modifier.setDishModifierId(modifierId);
                    modifier.setExtraPrice(modifierExtraPrice(modifierId));
                    modifierRepository.save(modifier);
                }
            }

            log.info("Ítem {} agregado a comanda {}. Platillo: {}, cantidad: {}",
                    item.getOrderItemId(), ticket.getOrderTicketId(), line.dishId(), line.quantity());
        }

        ticketRepository.save(ticket);
        log.info("Comanda {} enviada para cuenta {}. Ronda: {}",
                ticket.getOrderTicketId(), accountId, ticket.getSubmittedAt());

        return OrderTicketView.from(ticket);
    }

    // --- Ciclo de vida del ítem ---------------------------------------------

    /**
     * Actualiza el estado de un ítem de comanda. La máquina de estados es:
     * RECEIVED -> IN_PREPARATION -> READY -> DELIVERED
     */
    @Transactional
    public OrderItemView updateStatus(Long orderItemId, UpdateOrderItemStatusDTO request)
    {
        var item = itemForUpdate(orderItemId);
        var currentStatus = item.getStatus();
        var targetStatus = com.cunoc.restaurant.ordering.model.OrderItemStatus.valueOf(request.status().name());

        // Validar que el ítem no esté en estado final
        if (currentStatus == OrderItemStatus.DELIVERED)
            throw new BusinessException(ErrorCode.ORDER_ITEM_ALREADY_DELIVERED,
                    "El ítem " + orderItemId + " ya fue entregado.");

        if (currentStatus == OrderItemStatus.CANCELLED)
            throw new BusinessException(ErrorCode.ORDER_ITEM_ALREADY_CANCELLED,
                    "El ítem " + orderItemId + " ya fue cancelado.");

        // Validar transición válida
        var allowedTargets = ITEM_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus))
            throw new BusinessException(ErrorCode.INVALID_ORDER_ITEM_TRANSITION,
                    "No se puede cambiar el ítem " + orderItemId + " de " + currentStatus + " a " + targetStatus + ".");

        item.setStatus(targetStatus);

        // Marcar timestamps según el estado
        if (targetStatus == OrderItemStatus.READY)
            item.setReadyAt(LocalDateTime.now());
        else if (targetStatus == OrderItemStatus.DELIVERED)
            item.setDeliveredAt(LocalDateTime.now());

        itemRepository.save(item);
        log.info("Ítem {} cambiado de {} a {}", orderItemId, currentStatus, targetStatus);

        return OrderItemView.from(item);
    }

    /**
     * Actualiza cantidad, modificadores o nota de un ítem (solo si está RECEIVED).
     */
    @Transactional
    public OrderItemView update(Long orderItemId, UpdateOrderItemDTO request)
    {
        var item = itemForUpdate(orderItemId);

        if (item.getStatus() != OrderItemStatus.RECEIVED)
            throw new BusinessException(ErrorCode.ORDER_ITEM_IN_PREPARATION,
                    "Solo se pueden editar ítems en estado RECIBIDO. Estado actual: " + item.getStatus() + ".");

        item.setQuantity(request.quantity());
        item.setNote(request.note());
        itemRepository.save(item);

        log.info("Ítem {} actualizado. Nueva cantidad: {}", orderItemId, request.quantity());

        return OrderItemView.from(item);
    }

    /**
     * Elimina un ítem de la comanda (solo si está RECEIVED) y devuelve stock.
     */
    @Transactional
    public void delete(Long orderItemId)
    {
        var item = itemForUpdate(orderItemId);

        if (item.getStatus() != OrderItemStatus.RECEIVED)
            throw new BusinessException(ErrorCode.ORDER_ITEM_IN_PREPARATION,
                    "Solo se pueden eliminar ítems en estado RECIBIDO. Estado actual: " + item.getStatus() + ".");

        // Devolver stock al inventario
        inventoryService.reverseSaleConsumption(orderItemId, CurrentUser.id());

        // Eliminar modificadores asociados
        modifierRepository.findByOrderItemOrderItemId(orderItemId)
                .forEach(modifierRepository::delete);

        itemRepository.delete(item);
        log.info("Ítem {} eliminado y stock devuelto.", orderItemId);
    }

    /**
     * Marca un ítem como no disponible (solo cocina, solo si está RECEIVED).
     */
    @Transactional
    public void markUnavailable(Long orderItemId)
    {
        var item = itemForUpdate(orderItemId);

        if (item.getStatus() != OrderItemStatus.RECEIVED)
            throw new BusinessException(ErrorCode.ORDER_ITEM_IN_PREPARATION,
                    "Solo se pueden marcar como no disponibles ítems en estado RECIBIDO. Estado actual: " + item.getStatus() + ".");

        item.setStatus(OrderItemStatus.UNAVAILABLE);
        itemRepository.save(item);

        // Devolver stock al inventario
        inventoryService.reverseSaleConsumption(orderItemId, CurrentUser.id());

        log.info("Ítem {} marcado como no disponible y stock devuelto.", orderItemId);
    }

    /**
     * Cancela un ítem de la comanda (solo ADMIN). No devuelve stock
     * (el insumo ya se gastó en la cocina).
     */
    @Transactional
    public void cancel(Long orderItemId, CancelOrderItemDTO request)
    {
        var item = itemForUpdate(orderItemId);

        if (item.getStatus() == OrderItemStatus.CANCELLED)
            throw new BusinessException(ErrorCode.ORDER_ITEM_ALREADY_CANCELLED,
                    "El ítem " + orderItemId + " ya fue cancelado.");

        item.setStatus(OrderItemStatus.CANCELLED);
        item.setCancelledBy(CurrentUser.id());
        item.setCancellationReason(request.reason());
        itemRepository.save(item);

        // No se devuelve stock: el insumo ya se gastó en la cocina (03 · §5.5)
        log.info("Ítem {} cancelado por usuario {}. Motivo: {}", orderItemId, CurrentUser.id(), request.reason());
    }

    // --- Consultas ----------------------------------------------------------

    /**
     * Busca ítems en la cola de cocina con filtros.
     */
    @Transactional(readOnly = true)
    public Page<OrderItemView> searchQueue(com.cunoc.restaurant.ordering.model.OrderItemStatus status, Long tableId,
                                           Long waiterId, Boolean overdue, Pageable pageable)
    {
        return itemRepository.searchQueue(status, tableId, waiterId, pageable)
                .map(item ->
                {
                    var view = OrderItemView.from(item);
                    // Calcular overdue dinámicamente
                    if (overdue != null && overdue)
                    {
                        boolean isOverdue = calculateOverdue(item);
                        return new OrderItemView(
                                view.orderItemId(), view.dishId(), view.dishName(),
                                view.modifiers(), view.comboId(), view.quantity(),
                                view.unitPrice(), view.unitCost(), view.note(),
                                view.status(), view.submittedAt(), view.readyAt(),
                                view.deliveredAt(), isOverdue, view.accountSplitCreatedAt());
                    }
                    return view;
                });
    }

    /**
     * Obtiene el detalle de una comanda.
     */
    @Transactional(readOnly = true)
    public OrderTicketView findTicket(Long ticketId)
    {
        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND,
                        "No existe la comanda " + ticketId + "."));
        return OrderTicketView.from(ticket);
    }

    // --- Métodos auxiliares -------------------------------------------------

    private boolean calculateOverdue(OrderItem item)
    {
        // El cálculo real de overdue requiere prepMinutes del dish, que viene de menu (B2).
        // Por ahora retornamos false; se implementará cuando B2 esté disponible.
        return false;
    }

    private BigDecimal dishSalePrice(Long dishId)
    {
        // Por ahora retorna BigDecimal.ZERO; se implementará cuando B2 esté disponible
        // con MenuService.dishBrief()
        return BigDecimal.ZERO;
    }

    private BigDecimal modifierExtraPrice(Long modifierId)
    {
        // Por ahora retorna BigDecimal.ZERO; se implementará cuando B2 esté disponible
        // con MenuService.modifierBrief()
        return BigDecimal.ZERO;
    }
    @Transactional()
    private TableAccount accountForUpdate(Long accountId)
    {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "No existe la cuenta " + accountId + "."));
    }

    private OrderItem itemForUpdate(Long orderItemId)
    {
        return itemRepository.findById(orderItemId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_ITEM_NOT_FOUND,
                        "No existe el ítem " + orderItemId + "."));
    }
}
