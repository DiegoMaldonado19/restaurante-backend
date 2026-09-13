package com.cunoc.restaurant.report;

import com.cunoc.restaurant.billing.model.Invoice;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.report.dto.CashShiftRowView;
import com.cunoc.restaurant.report.dto.DishProfitabilityRowView;
import com.cunoc.restaurant.report.dto.DishRankingRowView;
import com.cunoc.restaurant.report.dto.InventoryRowView;
import com.cunoc.restaurant.report.dto.LoyaltyRowView;
import com.cunoc.restaurant.report.dto.SalesRowView;
import com.cunoc.restaurant.report.dto.StockReconciliationRowView;
import com.cunoc.restaurant.report.dto.TableOccupancyRowView;
import com.cunoc.restaurant.report.dto.WaiterPerformanceRowView;
import com.cunoc.restaurant.report.dto.WasteRowView;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Las nueve consultas de reporte, mas la de reconciliacion de inventario.
 *
 * Extiende Repository y no JpaRepository a proposito: el marcador de Spring Data no
 * expone save() ni delete(), asi que la regla "report nunca escribe" la garantiza el
 * tipo y no la disciplina de quien lo lee.
 *
 * Es el unico modulo autorizado a unir tablas de otros. Las uniones van con ON explicito
 * porque las entidades guardan las claves ajenas como Long planos y no como @ManyToOne;
 * Hibernate lo admite entre entidades sin asociacion declarada.
 *
 * El rango se consulta siempre como [from, to): asi el ultimo dia entra completo y la
 * comparacion sigue usando el indice ix_invoice_issued.
 */
public interface ReportRepository extends Repository<Invoice, Long>
{
    /**
     * Ventas por franja, a grano de linea. Se filtra por categoria y por mesero, y no se
     * abre por ellos: "por categoria de platillo" solo es expresable en la linea, porque
     * descartar una factura entera por contener un postre inflaria el total.
     *
     * La expresion del CASE se repite en el GROUP BY porque HQL no permite agrupar por el
     * alias de una proyeccion construida con SELECT new.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.SalesRowView(
                      CASE WHEN :groupBy = 'month' THEN format(i.issuedAt as 'yyyy-MM')
                           WHEN :groupBy = 'week'  THEN format(i.issuedAt as 'yyyy-ww')
                           ELSE                         format(i.issuedAt as 'yyyy-MM-dd') END,
                      COUNT(DISTINCT i.invoiceId),
                      SUM(oi.quantity),
                      SUM(oi.quantity * oi.unitPrice),
                      SUM(oi.quantity * oi.unitCost),
                      SUM(oi.quantity * (oi.unitPrice - oi.unitCost)))
             FROM OrderItem oi
             JOIN oi.ticket t
             JOIN t.account ta
             JOIN Invoice i ON i.tableAccountId = ta.tableAccountId
             JOIN Dish d    ON d.dishId = oi.dishId
            WHERE i.status = com.cunoc.restaurant.billing.model.InvoiceStatus.ISSUED
              AND oi.status = com.cunoc.restaurant.ordering.model.OrderItemStatus.DELIVERED
              AND i.issuedAt >= :from AND i.issuedAt < :to
              AND (:categoryId IS NULL OR d.category.dishCategoryId = :categoryId)
              AND (:waiterId   IS NULL OR i.waiterId = :waiterId)
            GROUP BY CASE WHEN :groupBy = 'month' THEN format(i.issuedAt as 'yyyy-MM')
                          WHEN :groupBy = 'week'  THEN format(i.issuedAt as 'yyyy-ww')
                          ELSE                         format(i.issuedAt as 'yyyy-MM-dd') END
           """)
    List<SalesRowView> sales(@Param("from")       LocalDateTime from,
                             @Param("to")         LocalDateTime to,
                             @Param("groupBy")    String        groupBy,
                             @Param("categoryId") Long          categoryId,
                             @Param("waiterId")   Long          waiterId);

    /**
     * Platillos mas y menos vendidos. El LEFT JOIN y el CASE existen para que un platillo
     * sin ninguna venta aparezca con cero: es justamente "el menos vendido" que pide el
     * enunciado, y con un JOIN interno nunca saldria. El orden lo decide el service.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.DishRankingRowView(
                      d.dishId, d.name, c.name,
                      COALESCE(SUM(CASE WHEN i.invoiceId IS NULL THEN 0 ELSE oi.quantity END), 0L),
                      COALESCE(SUM(CASE WHEN i.invoiceId IS NULL THEN 0
                                        ELSE oi.quantity * oi.unitPrice END), 0),
                      COALESCE(SUM(CASE WHEN i.invoiceId IS NULL THEN 0
                                        ELSE oi.quantity * oi.unitCost END), 0),
                      COALESCE(SUM(CASE WHEN i.invoiceId IS NULL THEN 0
                                        ELSE oi.quantity * (oi.unitPrice - oi.unitCost) END), 0))
             FROM Dish d
             JOIN d.category c
             LEFT JOIN OrderItem oi
                    ON oi.dishId = d.dishId
                   AND oi.status = com.cunoc.restaurant.ordering.model.OrderItemStatus.DELIVERED
             LEFT JOIN OrderTicket  t  ON t  = oi.ticket
             LEFT JOIN TableAccount ta ON ta = t.account
             LEFT JOIN Invoice i
                    ON i.tableAccountId = ta.tableAccountId
                   AND i.status = com.cunoc.restaurant.billing.model.InvoiceStatus.ISSUED
                   AND i.issuedAt >= :from AND i.issuedAt < :to
            WHERE d.active = TRUE
              AND (:categoryId IS NULL OR c.dishCategoryId = :categoryId)
            GROUP BY d.dishId, d.name, c.name
           """)
    List<DishRankingRowView> dishRanking(@Param("from")       LocalDateTime from,
                                         @Param("to")         LocalDateTime to,
                                         @Param("categoryId") Long          categoryId);

    /**
     * Rentabilidad por platillo. El costo sale ponderado del unit_cost congelado en cada
     * linea, no de la receta de hoy: es para lo que existe esa columna.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.DishProfitabilityRowView(
                      d.dishId, d.name, c.name, d.salePrice,
                      SUM(oi.quantity * oi.unitPrice) / SUM(oi.quantity),
                      SUM(oi.quantity * oi.unitCost)  / SUM(oi.quantity),
                      (SUM(oi.quantity * oi.unitPrice) - SUM(oi.quantity * oi.unitCost))
                          / SUM(oi.quantity),
                      CASE WHEN SUM(oi.quantity * oi.unitPrice) = 0 THEN 0
                           ELSE (SUM(oi.quantity * oi.unitPrice) - SUM(oi.quantity * oi.unitCost))
                                * 100 / SUM(oi.quantity * oi.unitPrice) END,
                      SUM(oi.quantity),
                      SUM(oi.quantity * (oi.unitPrice - oi.unitCost)))
             FROM OrderItem oi
             JOIN oi.ticket t
             JOIN t.account ta
             JOIN Invoice i ON i.tableAccountId = ta.tableAccountId
             JOIN Dish d    ON d.dishId = oi.dishId
             JOIN d.category c
            WHERE i.status = com.cunoc.restaurant.billing.model.InvoiceStatus.ISSUED
              AND oi.status = com.cunoc.restaurant.ordering.model.OrderItemStatus.DELIVERED
              AND i.issuedAt >= :from AND i.issuedAt < :to
              AND (:categoryId IS NULL OR c.dishCategoryId = :categoryId)
            GROUP BY d.dishId, d.name, c.name, d.salePrice
            ORDER BY SUM(oi.quantity * (oi.unitPrice - oi.unitCost)) DESC
           """)
    List<DishProfitabilityRowView> dishProfitability(@Param("from")       LocalDateTime from,
                                                     @Param("to")         LocalDateTime to,
                                                     @Param("categoryId") Long          categoryId);

    /**
     * Existencias valoradas. Solo insumos activos: uno dado de baja no tiene saldo
     * utilizable e inflaria el valor total del inventario.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.InventoryRowView(
                      s.supplyId, s.name, c.name, s.measureUnit,
                      s.currentStock, s.minStock, s.maxStock, s.unitCost,
                      s.currentStock * s.unitCost,
                      CASE WHEN s.currentStock <= s.minStock THEN TRUE ELSE FALSE END)
             FROM Supply s
             JOIN s.category c
            WHERE s.active = TRUE
              AND (:categoryId IS NULL OR c.supplyCategoryId = :categoryId)
              AND (:lowStockOnly = FALSE OR s.currentStock <= s.minStock)
            ORDER BY c.name, s.name
           """)
    List<InventoryRowView> inventory(@Param("lowStockOnly") boolean lowStockOnly,
                                     @Param("categoryId")   Long    categoryId);

    /**
     * Mermas del periodo. La cantidad se guarda negativa, de ahi el ABS; y el costo se
     * toma del movimiento cuando lo trae y del insumo cuando no, porque registerWaste no
     * lo guardaba: sin el COALESCE la columna de costo perdido saldria vacia.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.WasteRowView(
                      s.supplyId, s.name, c.name, m.wasteReason, s.measureUnit,
                      SUM(ABS(m.quantity)),
                      SUM(ABS(m.quantity) * COALESCE(m.unitCost, s.unitCost)),
                      COUNT(m))
             FROM StockMovement m
             JOIN m.supply s
             JOIN s.category c
            WHERE m.movementType = com.cunoc.restaurant.inventory.model.MovementType.WASTE
              AND m.createdAt >= :from AND m.createdAt < :to
              AND (:supplyId IS NULL OR s.supplyId = :supplyId)
            GROUP BY s.supplyId, s.name, c.name, m.wasteReason, s.measureUnit
            ORDER BY SUM(ABS(m.quantity) * COALESCE(m.unitCost, s.unitCost)) DESC
           """)
    List<WasteRowView> waste(@Param("from")     LocalDateTime from,
                             @Param("to")       LocalDateTime to,
                             @Param("supplyId") Long          supplyId);

    /**
     * Ocupacion por franja horaria. Solo cuentas cerradas: una cancelada nunca ocupo la
     * mesa y una fusionada no tiene closed_at propio, asi que su estancia no es medible.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.TableOccupancyRowView(
                      hour(ta.openedAt),
                      COUNT(ta),
                      SUM(ta.guestCount),
                      AVG(timestampdiff(minute, ta.openedAt, ta.closedAt)))
             FROM TableAccount ta
             JOIN RestaurantTable rt ON rt.restaurantTableId = ta.restaurantTableId
            WHERE ta.openedAt >= :from AND ta.openedAt < :to
              AND ta.status = com.cunoc.restaurant.ordering.model.AccountStatus.CLOSED
              AND (:zone IS NULL OR rt.zone = :zone)
            GROUP BY hour(ta.openedAt)
            ORDER BY hour(ta.openedAt)
           """)
    List<TableOccupancyRowView> tableOccupancy(@Param("from") LocalDateTime from,
                                               @Param("to")   LocalDateTime to,
                                               @Param("zone") TableZone     zone);

    /**
     * Desempeno por mesero. Los tiempos y las calificaciones van en subconsultas
     * correlacionadas y no en el FROM: unir facturas con calificaciones y con lineas
     * multiplicaria las filas e inflaria SUM(subtotal), que es el clasico abanico de
     * agregados.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.WaiterPerformanceRowView(
                      u.userId, u.fullName,
                      COUNT(i), SUM(i.subtotal), SUM(i.tipAmount),
                      (SELECT AVG(timestampdiff(minute, oi.submittedAt, oi.deliveredAt))
                         FROM OrderItem oi
                         JOIN oi.ticket t
                        WHERE t.waiterId = u.userId
                          AND oi.deliveredAt IS NOT NULL
                          AND oi.submittedAt >= :from AND oi.submittedAt < :to),
                      (SELECT AVG(r.score) FROM ServiceRating r
                        WHERE r.waiterId = u.userId
                          AND r.createdAt >= :from AND r.createdAt < :to),
                      (SELECT COUNT(r) FROM ServiceRating r
                        WHERE r.waiterId = u.userId
                          AND r.createdAt >= :from AND r.createdAt < :to))
             FROM Invoice i
             JOIN AppUser u ON u.userId = i.waiterId
            WHERE i.status = com.cunoc.restaurant.billing.model.InvoiceStatus.ISSUED
              AND i.issuedAt >= :from AND i.issuedAt < :to
            GROUP BY u.userId, u.fullName
            ORDER BY SUM(i.subtotal) DESC
           """)
    List<WaiterPerformanceRowView> waiterPerformance(@Param("from") LocalDateTime from,
                                                     @Param("to")   LocalDateTime to);

    /**
     * Fidelizacion. Los puntos redimidos se guardan negativos, por eso se reportan con el
     * signo invertido. Las visitas se cuentan por tipo de transaccion y no por factura,
     * porque las redenciones se registran sin invoice_id.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.LoyaltyRowView(
                      cu.customerId, cu.fullName, cu.phone,
                      SUM(CASE WHEN lt.transactionType
                                  = com.cunoc.restaurant.customer.model.LoyaltyTransactionType.ACCRUAL
                               THEN 1L ELSE 0L END),
                      SUM(CASE WHEN lt.points > 0 THEN lt.points  ELSE 0 END),
                      SUM(CASE WHEN lt.points < 0 THEN -lt.points ELSE 0 END),
                      SUM(lt.points))
             FROM LoyaltyTransaction lt
             JOIN lt.customer cu
            WHERE lt.createdAt >= :from AND lt.createdAt < :to
            GROUP BY cu.customerId, cu.fullName, cu.phone
            ORDER BY SUM(CASE WHEN lt.points > 0 THEN lt.points ELSE 0 END) DESC
           """)
    List<LoyaltyRowView> loyalty(@Param("from") LocalDateTime from,
                                 @Param("to")   LocalDateTime to,
                                 Pageable       pageable);

    /** Cuadres del periodo. Sin agregados: cash_shift ya guarda esperado, contado y diferencia. */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.CashShiftRowView(
                      cs.cashShiftId, u.userId, u.fullName, cs.openedAt, cs.closedAt,
                      cs.openingBalance, cs.expectedCash, cs.countedCash, cs.difference, cs.status)
             FROM CashShift cs
             JOIN AppUser u ON u.userId = cs.cashierId
            WHERE cs.openedAt >= :from AND cs.openedAt < :to
              AND (:cashierId IS NULL OR cs.cashierId = :cashierId)
            ORDER BY cs.openedAt DESC
           """)
    List<CashShiftRowView> cashShifts(@Param("from")      LocalDateTime from,
                                      @Param("to")        LocalDateTime to,
                                      @Param("cashierId") Long          cashierId);

    /**
     * Descuadres entre la columna current_stock y el libro mayor. Tiene que devolver cero
     * filas: applyMovement es el unico escritor de esa columna, y si alguna vez se escribe
     * por otro camino, aqui se ve. No es endpoint porque el contrato esta cerrado en nueve
     * reportes; la comprueba una prueba en cada build.
     */
    @Query("""
           SELECT new com.cunoc.restaurant.report.dto.StockReconciliationRowView(
                      s.supplyId, s.name, s.currentStock, COALESCE(SUM(m.quantity), 0))
             FROM Supply s
             LEFT JOIN StockMovement m ON m.supply = s
            GROUP BY s.supplyId, s.name, s.currentStock
           HAVING s.currentStock <> COALESCE(SUM(m.quantity), 0)
           """)
    List<StockReconciliationRowView> stockReconciliation();
}
