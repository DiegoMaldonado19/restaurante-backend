package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.iam.AppUserService;
import com.cunoc.restaurant.menu.MenuService;
import com.cunoc.restaurant.menu.ModifierService;
import com.cunoc.restaurant.menu.dto.ModifierView;
import com.cunoc.restaurant.ordering.dto.AccountSplitView;
import com.cunoc.restaurant.ordering.dto.DishModifierView;
import com.cunoc.restaurant.ordering.dto.OrderItemView;
import com.cunoc.restaurant.ordering.dto.OrderTicketView;
import com.cunoc.restaurant.ordering.dto.TableAccountView;
import com.cunoc.restaurant.ordering.model.AccountSplit;
import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.OrderItemModifier;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.ordering.model.OrderTicket;
import com.cunoc.restaurant.ordering.model.TableAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma las vistas de ordering resolviendo lo que las entidades no pueden leer:
 * el nombre del platillo y de los modificadores (menu) y el del mesero (iam).
 * overdue = submitted_at + prep_minutes + gracia, solo en cola.
 */
@Component
@RequiredArgsConstructor
class OrderViewAssembler
{
    private final MenuService                  menuService;
    private final ModifierService              modifierService;
    private final AppUserService               appUserService;
    private final OrderItemModifierRepository  modifierRepository;

    @Value("${restaurant.order.overdue-grace-minutes:5}")
    private int overdueGraceMinutes = 5;

    OrderItemView toItem(OrderItem item)
    {
        var dish = menuService.dishDetail(item.getDishId());

        return new OrderItemView(
                item.getOrderItemId(),
                item.getDishId(),
                dish.name(),
                modifiersOf(item),
                item.getComboId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitCost(),
                item.getNote(),
                item.getStatus(),
                item.getSubmittedAt(),
                item.getReadyAt(),
                item.getDeliveredAt(),
                isOverdue(item, dish.prepMinutes()),
                item.getSplit() != null ? item.getSplit().getCreatedAt() : null);
    }

    OrderTicketView toTicket(OrderTicket ticket)
    {
        var items = ticket.getOrderItems() == null ? List.<OrderItem>of() : ticket.getOrderItems();
        var derived = items.stream()
                .map(OrderItem::getStatus)
                .min(OrderItemStatus::compareTo)
                .orElse(OrderItemStatus.RECEIVED);

        return new OrderTicketView(
                ticket.getOrderTicketId(),
                ticket.getAccount().getTableAccountId(),
                ticket.getWaiterId(),
                ticket.getSubmittedAt(),
                items.stream().map(this::toItem).toList(),
                derived);
    }

    TableAccountView toAccount(TableAccount account)
    {
        var splits = account.getAccountSplits() == null ? List.<AccountSplit>of() : account.getAccountSplits();
        var tickets = account.getOrderTickets() == null ? List.<OrderTicket>of() : account.getOrderTickets();
        var splitTotal = splits.stream()
                .map(split -> split.getShareAmount() == null ? BigDecimal.ZERO : split.getShareAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var splitViews = splits.stream()
                .map(split -> toSplit(split, itemsAssignedTo(account, split)))
                .toList();

        return new TableAccountView(
                account.getTableAccountId(),
                account.getRestaurantTableId(),
                account.getGuestCount(),
                account.getStatus(),
                account.getOpenedAt(),
                account.getClosedAt(),
                new TableAccountView.SplitsInfo(splits.size(), splitTotal, splitViews),
                tickets.stream().map(this::toTicket).toList(),
                vigentesTotal(account),
                account.getWaiterId(),
                appUserService.findById(account.getWaiterId()).fullName());
    }

    AccountSplitView toSplit(AccountSplit split, List<OrderItem> items)
    {
        var assigned = items == null ? List.<OrderItem>of() : items;
        return new AccountSplitView(
                split.getAccountSplitId(),
                split.getLabel(),
                split.getMode(),
                split.getShareAmount(),
                split.getCreatedAt(),
                assigned.stream().map(this::toItem).toList());
    }

    Page<TableAccountView> toAccountPage(Page<TableAccount> page)
    {
        return page.map(this::toAccount);
    }

    /**
     * Suma unit_price × quantity de las lineas no CANCELLED y no UNAVAILABLE.
     * La usan running_total y el split BY_PERSON, para que mapa, detalle y cobro
     * hablen del mismo numero.
     */
    BigDecimal vigentesTotal(TableAccount account)
    {
        return vigentesTotal(itemsOf(account));
    }

    BigDecimal vigentesTotal(List<OrderItem> items)
    {
        var total = BigDecimal.ZERO;
        if (items == null)
            return total.setScale(2, RoundingMode.HALF_UP);

        for (var line : items)
        {
            if (!esVigente(line) || line.getUnitPrice() == null)
                continue;
            total = total.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    List<OrderItem> itemsOf(TableAccount account)
    {
        var tickets = account.getOrderTickets() == null ? List.<OrderTicket>of() : account.getOrderTickets();
        var result = new ArrayList<OrderItem>();
        for (var ticket : tickets)
        {
            if (ticket.getOrderItems() != null)
                result.addAll(ticket.getOrderItems());
        }
        return result;
    }

    List<OrderItem> itemsAssignedTo(TableAccount account, AccountSplit split)
    {
        if (account == null || split == null || split.getAccountSplitId() == null)
            return List.of();

        return itemsOf(account).stream()
                .filter(item -> item.getSplit() != null
                        && split.getAccountSplitId().equals(item.getSplit().getAccountSplitId()))
                .toList();
    }

    static boolean esVigente(OrderItem item)
    {
        return item.getStatus() != OrderItemStatus.CANCELLED
                && item.getStatus() != OrderItemStatus.UNAVAILABLE;
    }

    boolean isOverdue(OrderItem item, int prepMinutes)
    {
        if (item.getStatus() != OrderItemStatus.RECEIVED
                && item.getStatus() != OrderItemStatus.IN_PREPARATION
                && item.getStatus() != OrderItemStatus.READY)
            return false;
        if (item.getSubmittedAt() == null)
            return false;

        var due = item.getSubmittedAt().plusMinutes(prepMinutes).plusMinutes(overdueGraceMinutes);
        return due.isBefore(LocalDateTime.now());
    }

    private List<DishModifierView> modifiersOf(OrderItem item)
    {
        if (item.getOrderItemId() == null)
            return List.of();

        var applied = modifierRepository.findByOrderItemOrderItemId(item.getOrderItemId());
        if (applied.isEmpty())
            return List.of();

        var catalog = modifierService.findByDish(item.getDishId());

        return applied.stream()
                .map(row -> new DishModifierView(
                        row.getDishModifierId(),
                        modifierName(catalog, row),
                        row.getExtraPrice()))
                .toList();
    }

    private static String modifierName(List<ModifierView> catalog, OrderItemModifier row)
    {
        return catalog.stream()
                .filter(modifier -> modifier.dishModifierId().equals(row.getDishModifierId()))
                .map(ModifierView::name)
                .findFirst()
                .orElse("Modifier#" + row.getDishModifierId());
    }
}
