package com.cunoc.restaurant.report;

import com.cunoc.restaurant.billing.model.Invoice;
import com.cunoc.restaurant.billing.model.InvoiceStatus;
import com.cunoc.restaurant.inventory.model.MeasureUnit;
import com.cunoc.restaurant.inventory.model.MovementType;
import com.cunoc.restaurant.inventory.model.StockMovement;
import com.cunoc.restaurant.inventory.model.Supply;
import com.cunoc.restaurant.inventory.model.SupplyCategory;
import com.cunoc.restaurant.menu.model.Dish;
import com.cunoc.restaurant.menu.model.DishCategory;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.ordering.model.OrderTicket;
import com.cunoc.restaurant.ordering.model.TableAccount;
import com.cunoc.restaurant.report.dto.ReportRange;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las consultas de report son casi puro SQL: mockear el repositorio no probaria nada, asi
 * que estas van contra la base de verdad, la misma que CI levanta para el build.
 *
 * Cubren las exclusiones que un reporte de ventas mal escrito se come sin avisar —las
 * facturas anuladas y las lineas canceladas— y el descuadre de inventario.
 *
 * Los datos se siembran en 2027, donde V4__demo_data.sql no llega, para que las filas de
 * la demostracion no se mezclen con las aserciones. La transaccion se revierte al final.
 */
@SpringBootTest
@Transactional
class ReportQueryTest
{
    private static final LocalDateTime WHEN = LocalDateTime.of(2027, 3, 10, 13, 0);

    private static final ReportRange RANGE =
            new ReportRange(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31));

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ReportRepository reportRepository;

    private Long dishId;

    /**
     * Una cuenta con dos lineas entregadas y una cancelada, facturada y emitida; y otra
     * cuenta entera cuya factura quedo anulada.
     */
    @BeforeEach
    void seed()
    {
        var dishCategory = persist(dishCategory("Plato fuerte de prueba"));
        var dish         = persist(dish(dishCategory, "Hamburguesa de prueba", "55.00"));
        dishId = dish.getDishId();

        var goodAccount = persist(account());
        var goodTicket  = persist(ticket(goodAccount));
        persist(item(goodTicket, dishId, 2, OrderItemStatus.DELIVERED));
        persist(item(goodTicket, dishId, 5, OrderItemStatus.CANCELLED));
        persist(invoice(goodAccount, InvoiceStatus.ISSUED, "110.00"));

        var voidedAccount = persist(account());
        var voidedTicket  = persist(ticket(voidedAccount));
        persist(item(voidedTicket, dishId, 7, OrderItemStatus.DELIVERED));
        persist(invoice(voidedAccount, InvoiceStatus.VOIDED, "385.00"));

        entityManager.flush();
    }

    @Test
    void lasVentasExcluyenLasFacturasAnuladasYLasLineasCanceladas()
    {
        var rows = reportRepository.sales(RANGE.fromAt(), RANGE.toAt(), "day", null, null);

        assertThat(rows).hasSize(1);
        // Solo las 2 unidades entregadas y facturadas: ni las 5 canceladas ni las 7 anuladas.
        assertThat(rows.getFirst().units()).isEqualTo(2L);
        assertThat(rows.getFirst().sales()).isEqualByComparingTo("110.00");
    }

    @Test
    void elRankingCuentaSoloLoEntregadoYFacturado()
    {
        var rows = reportRepository.dishRanking(RANGE.fromAt(), RANGE.toAt(), null);

        assertThat(rows).anySatisfy(row -> {
            assertThat(row.dishId()).isEqualTo(dishId);
            assertThat(row.units()).isEqualTo(2L);
        });
    }

    /** Un platillo sin ninguna venta es "el menos vendido", y tiene que salir con cero. */
    @Test
    void elRankingIncluyeLosPlatillosQueNoSeVendieron()
    {
        var otherCategory = persist(dishCategory("Postre de prueba"));
        persist(dish(otherCategory, "Brownie de prueba", "30.00"));
        entityManager.flush();

        var rows = reportRepository.dishRanking(RANGE.fromAt(), RANGE.toAt(), null);

        assertThat(rows).anySatisfy(row -> {
            assertThat(row.dishName()).isEqualTo("Brownie de prueba");
            assertThat(row.units()).isZero();
        });
    }

    /**
     * Comprueba las dos direcciones a la vez: que el insumo descuadrado salga y que todo
     * lo demas —incluido lo que siembra V4— calle.
     */
    @Test
    void laReconciliacionSenalaSoloElInsumoDescuadrado()
    {
        var category = persist(supplyCategory());

        var balanced = persist(supply(category, "Insumo cuadrado de prueba", "10.000"));
        persist(movement(balanced, "10.000"));

        var broken = persist(supply(category, "Insumo descuadrado de prueba", "4.000"));
        persist(movement(broken, "6.000"));

        entityManager.flush();

        var rows = reportRepository.stockReconciliation();

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().name()).isEqualTo("Insumo descuadrado de prueba");
        assertThat(rows.getFirst().currentStock()).isEqualByComparingTo("4.000");
        assertThat(rows.getFirst().ledgerBalance()).isEqualByComparingTo("6.000");
    }

    // --- Constructores de datos de prueba -----------------------------------

    private <T> T persist(T entity)
    {
        entityManager.persist(entity);

        return entity;
    }

    private static DishCategory dishCategory(String name)
    {
        var category = new DishCategory();
        category.setName(name);
        category.setActive(true);

        return category;
    }

    private static Dish dish(DishCategory category, String name, String price)
    {
        var dish = new Dish();
        dish.setCategory(category);
        dish.setName(name);
        dish.setSalePrice(new BigDecimal(price));
        dish.setPrepMinutes(10);
        dish.setManualAvailable(true);
        dish.setActive(true);

        return dish;
    }

    private static TableAccount account()
    {
        var account = new TableAccount();
        account.setRestaurantTableId(1L);
        account.setWaiterId(1L);
        account.setGuestCount(2);
        account.setStatus(AccountStatus.CLOSED);
        account.setOpenedAt(WHEN);
        account.setClosedAt(WHEN.plusHours(1));

        return account;
    }

    private static OrderTicket ticket(TableAccount account)
    {
        var ticket = new OrderTicket();
        ticket.setAccount(account);
        ticket.setWaiterId(1L);
        ticket.setSubmittedAt(WHEN);

        return ticket;
    }

    private static OrderItem item(OrderTicket ticket, Long dishId, int quantity,
                                  OrderItemStatus status)
    {
        var item = new OrderItem();
        item.setTicket(ticket);
        item.setDishId(dishId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("55.00"));
        item.setUnitCost(new BigDecimal("12.00"));
        item.setStatus(status);
        item.setSubmittedAt(WHEN);

        if (status == OrderItemStatus.DELIVERED)
        {
            item.setReadyAt(WHEN.plusMinutes(15));
            item.setDeliveredAt(WHEN.plusMinutes(20));
        }

        return item;
    }

    private static Invoice invoice(TableAccount account, InvoiceStatus status, String subtotal)
    {
        var invoice = new Invoice();
        invoice.setInvoiceNumber(System.nanoTime());
        invoice.setTableAccountId(account.getTableAccountId());
        invoice.setRestaurantTableId(1L);
        invoice.setCashShiftId(1L);
        invoice.setCashierId(1L);
        invoice.setWaiterId(1L);
        invoice.setSubtotal(new BigDecimal(subtotal));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTipAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTotal(new BigDecimal(subtotal));
        invoice.setStatus(status);
        invoice.setIssuedAt(WHEN.plusHours(1));

        return invoice;
    }

    private static SupplyCategory supplyCategory()
    {
        var category = new SupplyCategory();
        category.setName("Categoria de prueba");
        category.setActive(true);

        return category;
    }

    private static Supply supply(SupplyCategory category, String name, String currentStock)
    {
        var supply = new Supply();
        supply.setCategory(category);
        supply.setName(name);
        supply.setMeasureUnit(MeasureUnit.KG);
        supply.setUnitCost(new BigDecimal("45.00"));
        supply.setCurrentStock(new BigDecimal(currentStock));
        supply.setMinStock(new BigDecimal("1.000"));
        supply.setActive(true);

        return supply;
    }

    private static StockMovement movement(Supply supply, String quantity)
    {
        var movement = new StockMovement();
        movement.setSupply(supply);
        movement.setMovementType(MovementType.PURCHASE);
        movement.setQuantity(new BigDecimal(quantity));
        movement.setUnitCost(new BigDecimal("45.00"));
        movement.setUserId(1L);
        movement.setCreatedAt(WHEN);

        return movement;
    }
}
