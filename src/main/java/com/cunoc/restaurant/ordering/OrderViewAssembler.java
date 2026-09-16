package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.iam.AppUserService;
import com.cunoc.restaurant.menu.MenuService;
import com.cunoc.restaurant.menu.ModifierService;
import com.cunoc.restaurant.menu.dto.ModifierView;
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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Arma las vistas de ordering resolviendo lo que las entidades no pueden leer:
 * el nombre del platillo y de los modificadores (menu) y el del mesero (iam).
 * overdue y running_total se dejan como estan: los calculan las fases 4 y 2.
 */
@Component
@RequiredArgsConstructor
class OrderViewAssembler
{
    private final MenuService                  menuService;
    private final ModifierService              modifierService;
    private final AppUserService               appUserService;
    private final OrderItemModifierRepository  modifierRepository;

    OrderItemView toItem(OrderItem item)
    {
        var dishName = menuService.dishDetail(item.getDishId()).name();

        return new OrderItemView(
                item.getOrderItemId(),
                item.getDishId(),
                dishName,
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
                false,
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

        return new TableAccountView(
                account.getTableAccountId(),
                account.getRestaurantTableId(),
                account.getGuestCount(),
                account.getStatus(),
                account.getOpenedAt(),
                account.getClosedAt(),
                new TableAccountView.SplitsInfo(splits.size(), splitTotal),
                tickets.stream().map(this::toTicket).toList(),
                splitTotal,
                account.getWaiterId(),
                appUserService.findById(account.getWaiterId()).fullName());
    }

    Page<TableAccountView> toAccountPage(Page<TableAccount> page)
    {
        return page.map(this::toAccount);
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
