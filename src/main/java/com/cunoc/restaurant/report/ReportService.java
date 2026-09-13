package com.cunoc.restaurant.report;

import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.report.dto.CashShiftRowView;
import com.cunoc.restaurant.report.dto.DishProfitabilityRowView;
import com.cunoc.restaurant.report.dto.DishRankingRowView;
import com.cunoc.restaurant.report.dto.InventoryRowView;
import com.cunoc.restaurant.report.dto.LoyaltyRowView;
import com.cunoc.restaurant.report.dto.ReportRange;
import com.cunoc.restaurant.report.dto.SalesRowView;
import com.cunoc.restaurant.report.dto.StockReconciliationRowView;
import com.cunoc.restaurant.report.dto.TableOccupancyRowView;
import com.cunoc.restaurant.report.dto.WaiterPerformanceRowView;
import com.cunoc.restaurant.report.dto.WasteRowView;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Solo lectura, siempre. El modulo no tiene tablas propias ni escribe en las ajenas. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService
{
    private final ReportRepository reportRepository;

    /**
     * Las etiquetas de periodo (yyyy-MM-dd, yyyy-ww, yyyy-MM) ordenan igual alfabetica que
     * cronologicamente, asi que basta comparar cadenas y no hace falta ordenar en la base.
     */
    public List<SalesRowView> sales(ReportRange range, String groupBy, Long categoryId, Long waiterId)
    {
        return reportRepository.sales(range.fromAt(), range.toAt(), groupBy, categoryId, waiterId)
                .stream()
                .sorted(Comparator.comparing(SalesRowView::period))
                .toList();
    }

    /** El sentido lo decide `order`, y el corte se hace aqui: la lista de platillos es corta. */
    public List<DishRankingRowView> dishRanking(ReportRange range, String order, int limit, Long categoryId)
    {
        var byUnits = Comparator.comparing(DishRankingRowView::units);
        var ranking = "bottom".equalsIgnoreCase(order) ? byUnits : byUnits.reversed();

        return reportRepository.dishRanking(range.fromAt(), range.toAt(), categoryId)
                .stream()
                .sorted(ranking.thenComparing(DishRankingRowView::dishName))
                .limit(limit)
                .toList();
    }

    public List<DishProfitabilityRowView> dishProfitability(ReportRange range, Long categoryId)
    {
        return reportRepository.dishProfitability(range.fromAt(), range.toAt(), categoryId);
    }

    public List<InventoryRowView> inventory(boolean lowStockOnly, Long categoryId)
    {
        return reportRepository.inventory(lowStockOnly, categoryId);
    }

    public List<WasteRowView> waste(ReportRange range, Long supplyId)
    {
        return reportRepository.waste(range.fromAt(), range.toAt(), supplyId);
    }

    public List<TableOccupancyRowView> tableOccupancy(ReportRange range, TableZone zone)
    {
        return reportRepository.tableOccupancy(range.fromAt(), range.toAt(), zone);
    }

    public List<WaiterPerformanceRowView> waiterPerformance(ReportRange range)
    {
        return reportRepository.waiterPerformance(range.fromAt(), range.toAt());
    }

    public List<LoyaltyRowView> loyalty(ReportRange range, int limit)
    {
        return reportRepository.loyalty(range.fromAt(), range.toAt(), PageRequest.of(0, limit));
    }

    public List<CashShiftRowView> cashShifts(ReportRange range, Long cashierId)
    {
        return reportRepository.cashShifts(range.fromAt(), range.toAt(), cashierId);
    }

    /** Debe devolver la lista vacia. La comprueba una prueba, no una pantalla. */
    public List<StockReconciliationRowView> stockReconciliation()
    {
        return reportRepository.stockReconciliation();
    }
}
