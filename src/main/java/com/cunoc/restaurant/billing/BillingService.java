package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.BillPreviewView;
import com.cunoc.restaurant.billing.dto.SplitPreviewView;
import com.cunoc.restaurant.billing.model.AccountSplit;
import com.cunoc.restaurant.billing.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService
{
    private final OrderTicketRepository         orderTicketRepository;
    private final OrderItemRepository            orderItemRepository;
    private final OrderItemModifierRepository    orderItemModifierRepository;
    private final AccountSplitRepository         accountSplitRepository;

    @Value("${restaurant.tax.percent}")
    private BigDecimal taxPercent;

    @Value("${restaurant.tip.suggested-percent}")
    private BigDecimal suggestedTipPercent;

    public BillPreviewView billPreview(Long accountId)
    {
        var ticketIds = orderTicketRepository.findByTableAccountId(accountId).stream()
                .map(t -> t.getOrderTicketId())
                .toList();

        var items = orderItemRepository.findByOrderTicketIdIn(ticketIds).stream()
                .filter(item -> !"CANCELLED".equals(item.getStatus()))
                .toList();

        var itemIds = items.stream().map(OrderItem::getOrderItemId).toList();

        var modifiersByItem = orderItemModifierRepository.findByOrderItemIdIn(itemIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getOrderItemId(),
                        java.util.stream.Collectors.mapping(m -> m.getExtraPrice(), java.util.stream.Collectors.toList())));

        var subtotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> subtotalBySplit = new HashMap<>();

        for (var item : items)
        {
            var modifiersTotal = modifiersByItem.getOrDefault(item.getOrderItemId(), List.of()).stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .add(modifiersTotal);

            subtotal = subtotal.add(lineTotal);

            if (item.getAccountSplitId() != null)
            {
                subtotalBySplit.merge(item.getAccountSplitId(), lineTotal, BigDecimal::add);
            }
        }

        var taxAmount = subtotal.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        var tipAmount = subtotal.multiply(suggestedTipPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        var total     = subtotal.add(taxAmount);

        var splitLabels = accountSplitRepository.findByTableAccountId(accountId).stream()
                .collect(java.util.stream.Collectors.toMap(AccountSplit::getAccountSplitId, AccountSplit::getLabel));

        var splits = subtotalBySplit.entrySet().stream()
                .map(e -> new SplitPreviewView(e.getKey(), splitLabels.get(e.getKey()), e.getValue()))
                .toList();

        return new BillPreviewView(accountId, subtotal, taxPercent, taxAmount,
                suggestedTipPercent, tipAmount, total, splits);
    }
}
