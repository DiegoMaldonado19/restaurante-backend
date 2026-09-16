package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.iam.AppUserService;
import com.cunoc.restaurant.iam.dto.UserView;
import com.cunoc.restaurant.iam.model.UserRole;
import com.cunoc.restaurant.iam.model.UserStatus;
import com.cunoc.restaurant.menu.MenuService;
import com.cunoc.restaurant.menu.ModifierService;
import com.cunoc.restaurant.menu.dto.DishDetailView;
import com.cunoc.restaurant.menu.dto.ModifierView;
import com.cunoc.restaurant.ordering.model.AccountSplit;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.OrderItemModifier;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.ordering.model.OrderTicket;
import com.cunoc.restaurant.ordering.model.TableAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderViewAssemblerTest
{
    private static final Long DISH_ID   = 100L;
    private static final Long ITEM_ID   = 200L;
    private static final Long WAITER_ID = 3L;

    private final MenuService                 menuService         = mock(MenuService.class);
    private final ModifierService             modifierService     = mock(ModifierService.class);
    private final AppUserService              appUserService      = mock(AppUserService.class);
    private final OrderItemModifierRepository modifierRepository  = mock(OrderItemModifierRepository.class);

    private final OrderViewAssembler views =
            new OrderViewAssembler(menuService, modifierService, appUserService, modifierRepository);

    private OrderItem item;

    @BeforeEach
    void setUp()
    {
        item = new OrderItem();
        item.setOrderItemId(ITEM_ID);
        item.setDishId(DISH_ID);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("55.00"));
        item.setUnitCost(new BigDecimal("12.00"));
        item.setStatus(OrderItemStatus.RECEIVED);
        item.setSubmittedAt(LocalDateTime.now());

        when(menuService.dishDetail(DISH_ID)).thenReturn(dish("Hamburguesa"));
        when(modifierRepository.findByOrderItemOrderItemId(ITEM_ID)).thenReturn(List.of());
        when(appUserService.findById(WAITER_ID)).thenReturn(waiter("Luis Gomez"));
    }

    @Test
    void toItemResuelveElNombreDelPlatillo()
    {
        var view = views.toItem(item);

        assertThat(view.dishName()).isEqualTo("Hamburguesa");
        assertThat(view.dishName()).isNotEqualTo("DishNamePlaceholder");
    }

    @Test
    void toItemHidrataModificadoresConElPrecioCongelado()
    {
        var applied = new OrderItemModifier();
        applied.setDishModifierId(9L);
        applied.setExtraPrice(new BigDecimal("5.00"));

        when(modifierRepository.findByOrderItemOrderItemId(ITEM_ID)).thenReturn(List.of(applied));
        when(modifierService.findByDish(DISH_ID)).thenReturn(List.of(
                new ModifierView(9L, DISH_ID, "Extra queso", new BigDecimal("4.00"), true)));

        var view = views.toItem(item);

        assertThat(view.modifiers()).hasSize(1);
        assertThat(view.modifiers().get(0).name()).isEqualTo("Extra queso");
        assertThat(view.modifiers().get(0).extraPrice()).isEqualByComparingTo("5.00");
    }

    @Test
    void toItemSinModificadoresNoConsultaElCatalogo()
    {
        views.toItem(item);

        verify(modifierService, never()).findByDish(DISH_ID);
        assertThat(views.toItem(item).modifiers()).isEmpty();
    }

    @Test
    void toItemDejaOverdueEnFalse()
    {
        assertThat(views.toItem(item).overdue()).isFalse();
    }

    @Test
    void toAccountResuelveElNombreDelMesero()
    {
        var view = views.toAccount(account());

        assertThat(view.waiterName()).isEqualTo("Luis Gomez");
        assertThat(view.waiterName()).isNotEqualTo("Waiter#3");
    }

    @Test
    void toAccountSumaLineasVigentesComoRunningTotal()
    {
        var pollo = priced(new BigDecimal("65.00"), 1, OrderItemStatus.RECEIVED);
        pollo.setOrderItemId(201L);
        pollo.setDishId(DISH_ID);
        when(modifierRepository.findByOrderItemOrderItemId(201L)).thenReturn(List.of());

        var account = account();
        var ticket = new OrderTicket();
        ticket.setAccount(account);
        ticket.setOrderItems(List.of(item, pollo));
        account.setOrderTickets(List.of(ticket));

        var split = new AccountSplit();
        split.setShareAmount(new BigDecimal("30.00"));
        account.setAccountSplits(List.of(split));

        var view = views.toAccount(account);

        assertThat(view.runningTotal()).isEqualByComparingTo("120.00");
        assertThat(view.splits().totalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void toAccountNoSumaLineasCanceladasNiNoDisponibles()
    {
        var cancelado = priced(new BigDecimal("55.00"), 1, OrderItemStatus.CANCELLED);
        cancelado.setOrderItemId(201L);
        cancelado.setDishId(DISH_ID);
        var noDisponible = priced(new BigDecimal("12.00"), 2, OrderItemStatus.UNAVAILABLE);
        noDisponible.setOrderItemId(202L);
        noDisponible.setDishId(DISH_ID);
        when(modifierRepository.findByOrderItemOrderItemId(201L)).thenReturn(List.of());
        when(modifierRepository.findByOrderItemOrderItemId(202L)).thenReturn(List.of());

        var account = account();
        var ticket = new OrderTicket();
        ticket.setAccount(account);
        ticket.setOrderItems(List.of(item, cancelado, noDisponible));
        account.setOrderTickets(List.of(ticket));

        assertThat(views.toAccount(account).runningTotal()).isEqualByComparingTo("55.00");
    }

    @Test
    void toTicketResuelveElNombreDeCadaLinea()
    {
        var ticket = new OrderTicket();
        ticket.setOrderTicketId(79L);
        ticket.setAccount(account());
        ticket.setWaiterId(WAITER_ID);
        ticket.setSubmittedAt(LocalDateTime.now());
        ticket.setOrderItems(List.of(item));

        var view = views.toTicket(ticket);

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).dishName()).isEqualTo("Hamburguesa");
    }

    private TableAccount account()
    {
        var account = new TableAccount();
        account.setTableAccountId(59L);
        account.setRestaurantTableId(1L);
        account.setWaiterId(WAITER_ID);
        account.setGuestCount(2);
        account.setStatus(AccountStatus.OPEN);
        account.setOpenedAt(LocalDateTime.now());
        account.setAccountSplits(new ArrayList<>());
        account.setOrderTickets(new ArrayList<>());
        return account;
    }

    private static OrderItem priced(BigDecimal unitPrice, int quantity, OrderItemStatus status)
    {
        var line = new OrderItem();
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setUnitCost(BigDecimal.ZERO);
        line.setStatus(status);
        line.setSubmittedAt(LocalDateTime.now());
        return line;
    }

    private static DishDetailView dish(String name)
    {
        return new DishDetailView(DISH_ID, 1L, "Fuertes", name, null, new BigDecimal("55.00"),
                null, 15, true, true, new BigDecimal("12.00"), new BigDecimal("78.00"), true, null);
    }

    private static UserView waiter(String fullName)
    {
        return new UserView(WAITER_ID, fullName, "mesero2", UserRole.WAITER, UserStatus.ACTIVE,
                LocalDateTime.now());
    }
}
