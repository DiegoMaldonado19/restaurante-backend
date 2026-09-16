package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.iam.AppUserService;
import com.cunoc.restaurant.inventory.InventoryService;
import com.cunoc.restaurant.inventory.dto.SupplyConsumption;
import com.cunoc.restaurant.menu.ComboService;
import com.cunoc.restaurant.menu.MenuService;
import com.cunoc.restaurant.menu.ModifierService;
import com.cunoc.restaurant.menu.dto.ComboDetailView;
import com.cunoc.restaurant.menu.dto.ComboItemView;
import com.cunoc.restaurant.ordering.dto.*;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.OrderItemModifier;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.ordering.model.OrderTicket;
import com.cunoc.restaurant.ordering.model.TableAccount;
import com.cunoc.restaurant.restaurant.RestaurantTableService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del servicio de comandas: envío, ciclo de vida de ítems y cola de cocina.
 */
class OrderServiceTest
{
    private static final Long ACCOUNT_ID = 1L;
    private static final Long TABLE_ID = 10L;
    private static final Long WAITER_ID = 5L;
    private static final Long DISH_ID = 100L;
    private static final Long DISH_ID_2 = 101L;
    private static final Long COMBO_ID = 9L;
    private static final Long ITEM_ID = 200L;

    private final TableAccountRepository accountRepository = mock(TableAccountRepository.class);
    private final OrderTicketRepository ticketRepository = mock(OrderTicketRepository.class);
    private final OrderItemRepository itemRepository = mock(OrderItemRepository.class);
    private final OrderItemModifierRepository modifierRepository = mock(OrderItemModifierRepository.class);
    private final MenuService menuService = mock(MenuService.class);
    private final ModifierService modifierService = mock(ModifierService.class);
    private final ComboService comboService = mock(ComboService.class);
    private final InventoryService inventoryService = mock(InventoryService.class);
    private final RestaurantTableService tableService = mock(RestaurantTableService.class);
    private final AppUserService appUserService = mock(AppUserService.class);
    private final OrderViewAssembler views =
            new OrderViewAssembler(menuService, modifierService, appUserService, modifierRepository);

    private final OrderService orderService =
            new OrderService(accountRepository, ticketRepository, itemRepository, modifierRepository,
                            menuService, modifierService, comboService, inventoryService, tableService, views);

    private final AtomicLong nextItemId = new AtomicLong(ITEM_ID);

    private TableAccount account;
    private OrderItem item;

    @BeforeEach
    void setUp()
    {
        // Mock security context
        var jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(String.valueOf(WAITER_ID));
        var auth = new UsernamePasswordAuthenticationToken(jwt, null,
                List.of(new SimpleGrantedAuthority("ROLE_WAITER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        account = new TableAccount();
        account.setTableAccountId(ACCOUNT_ID);
        account.setRestaurantTableId(TABLE_ID);
        account.setWaiterId(WAITER_ID);
        account.setStatus(AccountStatus.OPEN);
        account.setOpenedAt(LocalDateTime.now());

        item = new OrderItem();
        item.setOrderItemId(ITEM_ID);
        item.setDishId(DISH_ID);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("25.00"));
        item.setUnitCost(new BigDecimal("10.00"));
        item.setStatus(OrderItemStatus.RECEIVED);
        item.setSubmittedAt(LocalDateTime.now());
        var ticket = new OrderTicket();
        ticket.setAccount(account);
        ticket.setWaiterId(WAITER_ID);
        item.setTicket(ticket);

        when(accountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        nextItemId.set(ITEM_ID);
        // IDENTITY asigna el id en el save, y el descuento de stock depende de el para
        // referenciar el order_item. Sin simularlo, el mock no modela lo que pasa de verdad.
        when(itemRepository.save(any(OrderItem.class))).thenAnswer(inv ->
        {
            OrderItem saved = inv.getArgument(0);
            if (saved.getOrderItemId() == null) saved.setOrderItemId(nextItemId.getAndIncrement());

            return saved;
        });
        when(ticketRepository.save(any(OrderTicket.class))).thenAnswer(inv ->
        {
            OrderTicket saved = inv.getArgument(0);
            if (saved.getOrderTicketId() == null) saved.setOrderTicketId(81L);
            return saved;
        });
        when(modifierRepository.save(any(OrderItemModifier.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modifierRepository.findByOrderItemOrderItemId(anyLong())).thenReturn(List.of());

        // Mock de explodeRecipe y registerSaleConsumption
        when(menuService.explodeRecipe(any())).thenReturn(List.of(new SupplyConsumption(1L, BigDecimal.TEN)));
        when(menuService.productionCost(DISH_ID)).thenReturn(new BigDecimal("10.00"));
        when(menuService.productionCost(DISH_ID_2)).thenReturn(new BigDecimal("3.00"));
        when(menuService.dishDetail(DISH_ID)).thenReturn(new com.cunoc.restaurant.menu.dto.DishDetailView(
                DISH_ID, 1L, "Fuertes", "Hamburguesa", null, new BigDecimal("50.00"), null, 10,
                true, true, new BigDecimal("12.00"), new BigDecimal("76.00"), true, null));
        when(menuService.dishDetail(DISH_ID_2)).thenReturn(new com.cunoc.restaurant.menu.dto.DishDetailView(
                DISH_ID_2, 3L, "Bebidas", "Gaseosa", null, new BigDecimal("12.00"), null, 2,
                true, true, new BigDecimal("3.00"), new BigDecimal("75.00"), true, null));
    }

    @AfterEach
    void limpiarContextoDeSeguridad()
    {
        // El SecurityContextHolder es estatico y surefire reutiliza la JVM: sin esto la
        // autenticacion se filtra a la siguiente clase y UserControllerSecurityTest ve un
        // token donde esperaba una peticion anonima.
        SecurityContextHolder.clearContext();
    }


    // --- Enviar comanda ------------------------------------------------------

    @Test
    void submitCreaTicketYItems()
    {
        var line = new com.cunoc.restaurant.ordering.dto.OrderLineDTO(DISH_ID, null, 2, List.of(), null);
        var request = new SubmitOrderDTO(List.of(line));

        var result = orderService.submit(ACCOUNT_ID, request);

        assertThat(result).isNotNull();
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.orderTicketId()).isEqualTo(81L);
        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).dishName()).isEqualTo("Hamburguesa");
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.items().get(0).unitPrice()).isEqualByComparingTo("50.00");
        verify(inventoryService).registerSaleConsumption(any(), anyLong(), anyLong());

        // El precio de venta se congela en el item. Antes quedaba en cero y toda
        // factura totalizaba Q0.
        var guardado = org.mockito.ArgumentCaptor.forClass(OrderItem.class);
        verify(itemRepository).save(guardado.capture());
        assertThat(guardado.getValue().getUnitPrice()).isEqualByComparingTo("50.00");
        assertThat(guardado.getValue().getNote()).isNull();
        assertThat(guardado.getValue().getComboId()).isNull();
    }

    @Test
    void submitConNotaPersisteLaNota()
    {
        var line = new com.cunoc.restaurant.ordering.dto.OrderLineDTO(
                DISH_ID, null, 1, List.of(), "Sin cebolla");
        var result = orderService.submit(ACCOUNT_ID, new SubmitOrderDTO(List.of(line)));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).note()).isEqualTo("Sin cebolla");
        assertThat(result.items().get(0).comboId()).isNull();
    }

    @Test
    void submitConComboExplotaEnPlatillosYReparteElPrecio()
    {
        when(comboService.findById(COMBO_ID)).thenReturn(new ComboDetailView(
                COMBO_ID, "Combo Hamburguesa", null, new BigDecimal("60.00"), true,
                new BigDecimal("62.00"), new BigDecimal("2.00"),
                List.of(
                        new ComboItemView(DISH_ID, "Hamburguesa", new BigDecimal("50.00"), 1),
                        new ComboItemView(DISH_ID_2, "Gaseosa", new BigDecimal("12.00"), 1))));

        var line = new com.cunoc.restaurant.ordering.dto.OrderLineDTO(
                null, COMBO_ID, 1, List.of(), "Para compartir");
        var result = orderService.submit(ACCOUNT_ID, new SubmitOrderDTO(List.of(line)));

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).dishName()).isEqualTo("Hamburguesa");
        assertThat(result.items().get(0).comboId()).isEqualTo(COMBO_ID);
        assertThat(result.items().get(0).note()).isEqualTo("Para compartir");
        assertThat(result.items().get(0).unitPrice()).isEqualByComparingTo("48.39");
        assertThat(result.items().get(1).dishName()).isEqualTo("Gaseosa");
        assertThat(result.items().get(1).comboId()).isEqualTo(COMBO_ID);
        assertThat(result.items().get(1).note()).isEqualTo("Para compartir");
        assertThat(result.items().get(1).unitPrice()).isEqualByComparingTo("11.61");

        var suma = result.items().get(0).unitPrice()
                .multiply(BigDecimal.valueOf(result.items().get(0).quantity()))
                .add(result.items().get(1).unitPrice()
                        .multiply(BigDecimal.valueOf(result.items().get(1).quantity())));
        assertThat(suma).isEqualByComparingTo("60.00");
        verify(inventoryService, times(2)).registerSaleConsumption(any(), anyLong(), anyLong());
    }

    @Test
    void submitConComboInactivoFalla()
    {
        when(comboService.findById(COMBO_ID)).thenReturn(new ComboDetailView(
                COMBO_ID, "Combo apagado", null, new BigDecimal("60.00"), false,
                new BigDecimal("62.00"), new BigDecimal("2.00"),
                List.of(new ComboItemView(DISH_ID, "Hamburguesa", new BigDecimal("50.00"), 1))));

        var line = new com.cunoc.restaurant.ordering.dto.OrderLineDTO(null, COMBO_ID, 1, List.of(), null);

        assertThatThrownBy(() -> orderService.submit(ACCOUNT_ID, new SubmitOrderDTO(List.of(line))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DISH_UNAVAILABLE);
        verify(itemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void submitConComboYModificadoresFalla()
    {
        var line = new com.cunoc.restaurant.ordering.dto.OrderLineDTO(
                null, COMBO_ID, 1, List.of(1L), null);

        assertThatThrownBy(() -> orderService.submit(ACCOUNT_ID, new SubmitOrderDTO(List.of(line))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(comboService, never()).findById(anyLong());
    }

    @Test
    void submitConCuentaBillRequestedVuelveAOccupied()
    {
        account.setStatus(AccountStatus.BILL_REQUESTED);

        var line = new com.cunoc.restaurant.ordering.dto.OrderLineDTO(DISH_ID, null, 1, List.of(), null);
        var request = new SubmitOrderDTO(List.of(line));

        orderService.submit(ACCOUNT_ID, request);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.OPEN);
        verify(tableService).transitionTo(TABLE_ID, TableStatus.OCCUPIED);
    }

    @Test
    void submitConCuentaCerradaFalla()
    {
        account.setStatus(AccountStatus.CLOSED);

        var line = new com.cunoc.restaurant.ordering.dto.OrderLineDTO(DISH_ID, null, 1, List.of(), null);
        var request = new SubmitOrderDTO(List.of(line));

        assertThatThrownBy(() -> orderService.submit(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_OPEN);
    }

    @Test
    void findTicketResuelveElNombreDelPlatillo()
    {
        var ticket = new OrderTicket();
        ticket.setOrderTicketId(81L);
        ticket.setAccount(account);
        ticket.setWaiterId(WAITER_ID);
        ticket.setSubmittedAt(LocalDateTime.now());
        ticket.setOrderItems(List.of(item));
        when(ticketRepository.findById(81L)).thenReturn(Optional.of(ticket));

        var result = orderService.findTicket(81L);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).dishName()).isEqualTo("Hamburguesa");
    }

    // --- Máquina de estados del ítem -----------------------------------------

    @Test
    void itemRecibidoPuedeIrAPreparacion()
    {
        var request = new UpdateOrderItemStatusDTO(OrderItemStatus.IN_PREPARATION);

        var result = orderService.updateStatus(ITEM_ID, request);

        assertThat(result.status().name()).isEqualTo("IN_PREPARATION");
        assertThat(result.dishName()).isEqualTo("Hamburguesa");
    }

    @Test
    void itemPreparacionPuedeIrAReady()
    {
        item.setStatus(OrderItemStatus.IN_PREPARATION);

        var request = new UpdateOrderItemStatusDTO(OrderItemStatus.READY);

        var result = orderService.updateStatus(ITEM_ID, request);

        assertThat(result.status().name()).isEqualTo("READY");
    }

    @Test
    void itemReadyPuedeIrADelivered()
    {
        item.setStatus(OrderItemStatus.READY);

        var request = new UpdateOrderItemStatusDTO(OrderItemStatus.DELIVERED);

        var result = orderService.updateStatus(ITEM_ID, request);

        assertThat(result.status().name()).isEqualTo("DELIVERED");
        assertThat(result.deliveredAt()).isNotNull();
    }

    @Test
    void itemRecibidoNoPuedeIrADirectamente()
    {
        var request = new UpdateOrderItemStatusDTO(OrderItemStatus.DELIVERED);

        assertThatThrownBy(() -> orderService.updateStatus(ITEM_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_ITEM_TRANSITION);
    }

    @Test
    void itemEntregadoNoPuedeCambiarEstado()
    {
        item.setStatus(OrderItemStatus.DELIVERED);

        var request = new UpdateOrderItemStatusDTO(OrderItemStatus.IN_PREPARATION);

        assertThatThrownBy(() -> orderService.updateStatus(ITEM_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_ALREADY_DELIVERED);
    }

    @Test
    void itemCanceladoNoPuedeCambiarEstado()
    {
        item.setStatus(OrderItemStatus.CANCELLED);

        var request = new UpdateOrderItemStatusDTO(OrderItemStatus.READY);

        assertThatThrownBy(() -> orderService.updateStatus(ITEM_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_ALREADY_CANCELLED);
    }

    // --- Editar ítem ---------------------------------------------------------

    @Test
    void updateItemRecibidoFunciona()
    {
        var request = new UpdateOrderItemDTO(3, null, "Sin cebolla");

        var result = orderService.update(ITEM_ID, request);

        assertThat(result.quantity()).isEqualTo(3);
        assertThat(result.note()).isEqualTo("Sin cebolla");
        assertThat(result.dishName()).isEqualTo("Hamburguesa");
    }

    @Test
    void updateItemEnPreparacionNoPermitido()
    {
        item.setStatus(OrderItemStatus.IN_PREPARATION);

        var request = new UpdateOrderItemDTO(3, null, null);

        assertThatThrownBy(() -> orderService.update(ITEM_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_IN_PREPARATION);
    }

    // --- Eliminar ítem -------------------------------------------------------

    @Test
    void deleteItemRecibidoDevuelveStock()
    {
        orderService.delete(ITEM_ID);

        var orden = inOrder(inventoryService, itemRepository);
        orden.verify(inventoryService).reverseSaleConsumption(eq(ITEM_ID), eq(WAITER_ID));
        orden.verify(inventoryService).detachOrderItem(ITEM_ID);
        orden.verify(itemRepository).delete(item);
    }

    @Test
    void deleteItemDeCuentaAnuladaFalla()
    {
        account.setStatus(AccountStatus.CANCELLED);

        assertThatThrownBy(() -> orderService.delete(ITEM_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_OPEN);
        verify(inventoryService, never()).reverseSaleConsumption(anyLong(), anyLong());
        verify(itemRepository, never()).delete(item);
    }

    @Test
    void deleteItemEnPreparacionNoPermitido()
    {
        item.setStatus(OrderItemStatus.IN_PREPARATION);

        assertThatThrownBy(() -> orderService.delete(ITEM_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_IN_PREPARATION);
    }

    // --- Marcar no disponible ------------------------------------------------

    @Test
    void markUnavailableDevuelveStock()
    {
        orderService.markUnavailable(ITEM_ID);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.UNAVAILABLE);
        verify(inventoryService).reverseSaleConsumption(eq(ITEM_ID), eq(WAITER_ID));
    }

    @Test
    void updateStatusDeCuentaAnuladaFalla()
    {
        account.setStatus(AccountStatus.CANCELLED);

        assertThatThrownBy(() -> orderService.updateStatus(ITEM_ID,
                new UpdateOrderItemStatusDTO(OrderItemStatus.IN_PREPARATION)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_OPEN);
    }

    @Test
    void markUnavailableEnPreparacionNoPermitido()
    {
        item.setStatus(OrderItemStatus.IN_PREPARATION);

        assertThatThrownBy(() -> orderService.markUnavailable(ITEM_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_IN_PREPARATION);
    }

    // --- Cancelar ítem -------------------------------------------------------

    @Test
    void cancelItemNoDevuelveStock()
    {
        var request = new CancelOrderItemDTO("Cliente no quiere");

        orderService.cancel(ITEM_ID, request);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
        assertThat(item.getCancellationReason()).isEqualTo("Cliente no quiere");
        verify(inventoryService, never()).reverseSaleConsumption(anyLong(), anyLong());
    }

    @Test
    void cancelItemYaCanceladoFalla()
    {
        item.setStatus(OrderItemStatus.CANCELLED);

        var request = new CancelOrderItemDTO("Motivo");

        assertThatThrownBy(() -> orderService.cancel(ITEM_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_ALREADY_CANCELLED);
    }

    @Test
    void searchQueueConOverdueTrueSoloDevuelveVencidos()
    {
        item.setSubmittedAt(LocalDateTime.now().minusMinutes(30));

        var fresco = new OrderItem();
        fresco.setOrderItemId(201L);
        fresco.setDishId(DISH_ID);
        fresco.setQuantity(1);
        fresco.setUnitPrice(new BigDecimal("25.00"));
        fresco.setUnitCost(new BigDecimal("10.00"));
        fresco.setStatus(OrderItemStatus.RECEIVED);
        fresco.setSubmittedAt(LocalDateTime.now());

        var entregado = new OrderItem();
        entregado.setOrderItemId(202L);
        entregado.setDishId(DISH_ID);
        entregado.setQuantity(1);
        entregado.setUnitPrice(new BigDecimal("25.00"));
        entregado.setUnitCost(new BigDecimal("10.00"));
        entregado.setStatus(OrderItemStatus.DELIVERED);
        entregado.setSubmittedAt(LocalDateTime.now().minusMinutes(30));

        when(itemRepository.searchQueue(isNull(), isNull(), isNull(), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(item, fresco, entregado)));

        var page = orderService.searchQueue(null, null, null, true, PageRequest.of(0, 5));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).orderItemId()).isEqualTo(ITEM_ID);
        assertThat(page.getContent().get(0).overdue()).isTrue();
    }
}
