package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.inventory.dto.RegisterStockAdjustmentDTO;
import com.cunoc.restaurant.inventory.dto.RegisterStockEntryDTO;
import com.cunoc.restaurant.inventory.dto.RegisterStockWasteDTO;
import com.cunoc.restaurant.inventory.dto.SupplyConsumption;
import com.cunoc.restaurant.inventory.model.MeasureUnit;
import com.cunoc.restaurant.inventory.model.StockMovement;
import com.cunoc.restaurant.inventory.model.Supply;
import com.cunoc.restaurant.inventory.model.WasteReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La invariante del modulo: el saldo tiene que quedar siempre igual a la suma con signo
 * del libro mayor. Es la misma comprobacion que la consulta de reconciliacion corre
 * contra la base antes de la entrega, pero aqui cuesta milisegundos.
 */
class InventoryServiceTest
{
    private static final Long USER_ID   = 7L;
    private static final Long SUPPLY_ID = 1L;

    private final SupplyRepository        supplyRepository        = mock(SupplyRepository.class);
    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final SupplyCategoryService   supplyCategoryService   = mock(SupplyCategoryService.class);

    private final InventoryService inventoryService =
            new InventoryService(supplyRepository, stockMovementRepository, supplyCategoryService);

    private final List<StockMovement> ledger = new ArrayList<>();

    private Supply supply;

    @BeforeEach
    void setUp()
    {
        supply = new Supply();
        supply.setSupplyId(SUPPLY_ID);
        supply.setName("Carne molida");
        supply.setMeasureUnit(MeasureUnit.GRAM);
        supply.setUnitCost(new BigDecimal("0.00"));
        supply.setCurrentStock(new BigDecimal("0.000"));
        supply.setMinStock(new BigDecimal("500.000"));
        supply.setActive(true);

        ledger.clear();

        when(supplyRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(supply));
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocation ->
                {
                    var movement = invocation.<StockMovement>getArgument(0);
                    ledger.add(movement);

                    return movement;
                });
    }

    @Test
    void elSaldoSiempreCuadraConLaSumaDelLibroMayor()
    {
        inventoryService.registerEntry(entryOf("1000.000", "0.12"), USER_ID);
        inventoryService.registerWaste(wasteOf("150.000"), USER_ID);
        inventoryService.registerAdjustment(adjustmentOf("-50.000"), USER_ID);
        inventoryService.registerSaleConsumption(
                List.of(new SupplyConsumption(SUPPLY_ID, new BigDecimal("300.000"))), 42L, USER_ID);

        assertThat(supply.getCurrentStock()).isEqualByComparingTo("500.000");
        assertThat(supply.getCurrentStock()).isEqualByComparingTo(ledgerBalance());
    }

    @Test
    void laEntradaSobrescribeElCostoUnitario()
    {
        inventoryService.registerEntry(entryOf("1000.000", "0.12"), USER_ID);
        inventoryService.registerEntry(entryOf("1000.000", "0.15"), USER_ID);

        // Es el ultimo costo, no el promedio ponderado: lo que el enunciado pide en el catalogo.
        assertThat(supply.getUnitCost()).isEqualByComparingTo("0.15");
    }

    @Test
    void laMermaNoPuedeExcederElSaldo()
    {
        inventoryService.registerEntry(entryOf("100.000", "0.12"), USER_ID);

        assertThatThrownBy(() -> inventoryService.registerWaste(wasteOf("100.001"), USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.WASTE_EXCEEDS_STOCK);
    }

    @Test
    void laVentaSinSaldoSuficienteSeRechaza()
    {
        inventoryService.registerEntry(entryOf("100.000", "0.12"), USER_ID);

        assertThatThrownBy(() -> inventoryService.registerSaleConsumption(
                List.of(new SupplyConsumption(SUPPLY_ID, new BigDecimal("200.000"))), 42L, USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    void devolverUnItemRepuestoDosVecesNoDuplicaElStock()
    {
        inventoryService.registerEntry(entryOf("1000.000", "0.12"), USER_ID);
        inventoryService.registerSaleConsumption(
                List.of(new SupplyConsumption(SUPPLY_ID, new BigDecimal("300.000"))), 42L, USER_ID);

        when(stockMovementRepository.findByOrderItemId(42L)).thenAnswer(inv -> movementsOf(42L));

        inventoryService.reverseSaleConsumption(42L, USER_ID);
        inventoryService.reverseSaleConsumption(42L, USER_ID);   // cocina marco dos veces

        assertThat(supply.getCurrentStock()).isEqualByComparingTo("1000.000");
        assertThat(supply.getCurrentStock()).isEqualByComparingTo(ledgerBalance());
    }

    @Test
    void elInsumoInactivoNoTieneSaldoDisponible()
    {
        supply.setCurrentStock(new BigDecimal("1000.000"));
        supply.setActive(false);
        when(supplyRepository.findById(SUPPLY_ID)).thenReturn(Optional.of(supply));

        assertThat(inventoryService.availableStock(SUPPLY_ID)).isEqualByComparingTo("0");
    }

    private BigDecimal ledgerBalance()
    {
        return ledger.stream()
                .map(StockMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<StockMovement> movementsOf(Long orderItemId)
    {
        return ledger.stream()
                .filter(movement -> orderItemId.equals(movement.getOrderItemId()))
                .toList();
    }

    private RegisterStockEntryDTO entryOf(String quantity, String cost)
    {
        return new RegisterStockEntryDTO(SUPPLY_ID, new BigDecimal(quantity),
                                         new BigDecimal(cost), LocalDate.now());
    }

    private RegisterStockWasteDTO wasteOf(String quantity)
    {
        return new RegisterStockWasteDTO(SUPPLY_ID, new BigDecimal(quantity),
                                         WasteReason.EXPIRED, null);
    }

    private RegisterStockAdjustmentDTO adjustmentOf(String quantity)
    {
        return new RegisterStockAdjustmentDTO(SUPPLY_ID, new BigDecimal(quantity), "Conteo fisico");
    }
}
