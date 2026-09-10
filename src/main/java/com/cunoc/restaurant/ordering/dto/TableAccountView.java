package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.AccountSplit;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.TableAccount;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record TableAccountView(
        Long tableAccountId,
        Long restaurantTableId,
        Integer guestCount,
        AccountStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        SplitsInfo splits,
        List<OrderTicketView> tickets,
        BigDecimal runningTotal,
        String waiterName)
{
    public record SplitsInfo(int count, BigDecimal totalAmount) {}

    public static TableAccountView from(TableAccount account)
    {
        var splits = account.getAccountSplits();
        var total = splits != null ? splits.stream().map(AccountSplit::getShareAmount).reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        return new TableAccountView(
                account.getTableAccountId(),
                account.getRestaurantTableId(),
                account.getGuestCount(),
                account.getStatus(),
                account.getOpenedAt(),
                account.getClosedAt(),
                new SplitsInfo(splits != null ? splits.size() : 0, total),
                account.getOrderTickets().stream()
                        .map(OrderTicketView::from)
                        .collect(Collectors.toList()),
                total,
                "Waiter#" + account.getWaiterId());
    }

    public static Page<TableAccountView> page(Page<TableAccount> page)
    {
        return page.map(TableAccountView::from);
    }
}
