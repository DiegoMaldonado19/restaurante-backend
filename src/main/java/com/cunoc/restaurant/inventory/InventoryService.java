package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.inventory.dto.CreateSupplyDTO;
import com.cunoc.restaurant.inventory.dto.RegisterStockAdjustmentDTO;
import com.cunoc.restaurant.inventory.dto.RegisterStockEntryDTO;
import com.cunoc.restaurant.inventory.dto.RegisterStockWasteDTO;
import com.cunoc.restaurant.inventory.dto.StockMovementView;
import com.cunoc.restaurant.inventory.dto.SupplyConsumption;
import com.cunoc.restaurant.inventory.dto.SupplyDetailView;
import com.cunoc.restaurant.inventory.dto.SupplyView;
import com.cunoc.restaurant.inventory.dto.UpdateSupplyDTO;
import com.cunoc.restaurant.inventory.dto.UpdateSupplyStatusDTO;
import com.cunoc.restaurant.inventory.model.MovementType;
import com.cunoc.restaurant.inventory.model.StockMovement;
import com.cunoc.restaurant.inventory.model.Supply;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Insumos, saldo y libro mayor. La invariante del modulo es que applyMovement() es el
 * unico metodo que escribe supply.current_stock, y que lo hace en la misma transaccion
 * que inserta el stock_movement. Por eso SUM(quantity) por insumo tiene que dar siempre
 * el saldo, que es lo que comprueba la consulta de reconciliacion antes de la entrega.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService
{
    private final SupplyRepository         supplyRepository;
    private final StockMovementRepository  stockMovementRepository;
    private final SupplyCategoryService    supplyCategoryService;

    // --- Lo que consumen otros modulos --------------------------------------

    /** Un insumo inactivo no tiene saldo utilizable: es lo que vuelve no disponible al platillo. */
    @Transactional(readOnly = true)
    public BigDecimal availableStock(Long supplyId)
    {
        var supply = findOrFail(supplyId);

        return supply.isActive() ? supply.getCurrentStock() : BigDecimal.ZERO;
    }

    /** Descuento automatico al enviar una comanda. Lanza INSUFFICIENT_STOCK si no alcanza. */
    public void registerSaleConsumption(List<SupplyConsumption> lines, Long orderItemId, Long userId)
    {
        // Ordenado por insumo: dos comandas simultaneas toman los bloqueos en el mismo
        // orden y no se abrazan. Sin esto el interbloqueo aparece justo en hora pico.
        lines.stream()
                .sorted(Comparator.comparing(SupplyConsumption::supplyId))
                .forEach(line ->
                {
                    var movement = newMovement(findForUpdate(line.supplyId()),
                                               MovementType.SALE,
                                               line.quantity().negate(),
                                               userId);
                    movement.setOrderItemId(orderItemId);

                    applyMovement(movement, ErrorCode.INSUFFICIENT_STOCK);
                });
    }

    /**
     * Devuelve al inventario lo que consumio un item cancelado o marcado no disponible.
     * No borra filas: el libro mayor es inmutable, asi que compensa con el movimiento
     * contrario. Es idempotente porque trabaja sobre el neto, que ya es cero si se
     * revirtio antes: cocina puede marcar "no disponible" dos veces.
     */
    public void reverseSaleConsumption(Long orderItemId, Long userId)
    {
        Map<Long, BigDecimal> pending = new LinkedHashMap<>();

        for (var movement : stockMovementRepository.findByOrderItemId(orderItemId))
        {
            pending.merge(movement.getSupply().getSupplyId(), movement.getQuantity(), BigDecimal::add);
        }

        pending.forEach((supplyId, net) ->
        {
            if (net.signum() < 0)
            {
                var movement = newMovement(findForUpdate(supplyId),
                                           MovementType.SALE,
                                           net.negate(),
                                           userId);
                movement.setOrderItemId(orderItemId);
                movement.setReason("Devolucion por item cancelado o no disponible");

                applyMovement(movement, ErrorCode.INSUFFICIENT_STOCK);
            }
        });
    }

    // --- Catalogo de insumos ------------------------------------------------

    @Transactional(readOnly = true)
    public Page<SupplyView> search(Long categoryId, String search, Boolean active,
                                   Boolean lowStock, Pageable pageable)
    {
        return supplyRepository.search(categoryId, search, active, lowStock, pageable)
                .map(SupplyView::from);
    }

    @Transactional(readOnly = true)
    public SupplyDetailView findById(Long supplyId)
    {
        return SupplyDetailView.from(
                findOrFail(supplyId),
                stockMovementRepository.findTop10BySupplySupplyIdOrderByCreatedAtDesc(supplyId));
    }

    public SupplyView create(CreateSupplyDTO request)
    {
        if (supplyRepository.existsByNameIgnoreCase(request.name()))
        {
            throw new BusinessException(ErrorCode.SUPPLY_NAME_TAKEN,
                                        "Ya existe el insumo '" + request.name() + "'.");
        }

        var supply = new Supply();
        supply.setCategory(supplyCategoryService.findOrFail(request.supplyCategoryId()));
        supply.setName(request.name());
        supply.setMeasureUnit(request.measureUnit());
        supply.setUnitCost(request.unitCost());
        supply.setCurrentStock(BigDecimal.ZERO);   // El saldo solo sube con un movimiento.
        supply.setMinStock(request.minStock());
        supply.setMaxStock(request.maxStock());
        supply.setActive(true);

        return SupplyView.from(supplyRepository.save(supply));
    }

    /** No toca el saldo ni el costo: el saldo lo mueve un movimiento y el costo, una entrada. */
    public SupplyView update(Long supplyId, UpdateSupplyDTO request)
    {
        var supply = findOrFail(supplyId);

        if (supplyRepository.existsByNameIgnoreCaseAndSupplyIdNot(request.name(), supplyId))
        {
            throw new BusinessException(ErrorCode.SUPPLY_NAME_TAKEN,
                                        "Ya existe el insumo '" + request.name() + "'.");
        }

        supply.setCategory(supplyCategoryService.findOrFail(request.supplyCategoryId()));
        supply.setName(request.name());
        supply.setMeasureUnit(request.measureUnit());
        supply.setMinStock(request.minStock());
        supply.setMaxStock(request.maxStock());

        return SupplyView.from(supply);
    }

    /**
     * Desactivar siempre se permite. Un insumo inactivo deja de tener saldo utilizable,
     * asi que menu.isAvailable() marca solo el platillo como no disponible, que es lo que
     * pide el enunciado. Comprobar aqui si alguna receta lo usa exigiria inventory -> menu,
     * la arista que el grafo de modulos prohibe.
     */
    public SupplyView changeStatus(Long supplyId, UpdateSupplyStatusDTO request)
    {
        var supply = findOrFail(supplyId);
        supply.setActive(request.active());

        return SupplyView.from(supply);
    }

    // --- Kardex y movimientos -----------------------------------------------

    @Transactional(readOnly = true)
    public Page<StockMovementView> searchMovements(Long supplyId, MovementType movementType,
                                                   LocalDateTime from, LocalDateTime to,
                                                   Long userId, Pageable pageable)
    {
        return stockMovementRepository
                .search(supplyId, movementType, from, to, userId, kardexOrder(pageable))
                .map(StockMovementView::from);
    }

    /**
     * El kardex se lee de lo mas reciente a lo mas antiguo, y ese es el contrato del
     * endpoint, no una preferencia del cliente. created_at es DATETIME, con resolucion
     * de un segundo: sin el id como desempate, los movimientos de una misma entrada de
     * mercaderia salen en orden arbitrario.
     */
    private Pageable kardexOrder(Pageable pageable)
    {
        if (pageable.getSort().isSorted())
        {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                              Sort.by("createdAt").descending()
                                  .and(Sort.by("stockMovementId").descending()));
    }

    /** Entrada de mercaderia. Sobrescribe el costo unitario: es el ultimo, no el promedio. */
    public StockMovementView registerEntry(RegisterStockEntryDTO request, Long userId)
    {
        var supply   = findForUpdate(request.supplyId());
        var movement = newMovement(supply, MovementType.PURCHASE, request.quantity(), userId);
        movement.setUnitCost(request.purchaseCost());
        movement.setCreatedAt(timestampFor(request.entryDate()));

        applyMovement(movement, ErrorCode.INSUFFICIENT_STOCK);
        supply.setUnitCost(request.purchaseCost());

        return StockMovementView.from(movement);
    }

    public StockMovementView registerWaste(RegisterStockWasteDTO request, Long userId)
    {
        var movement = newMovement(findForUpdate(request.supplyId()),
                                   MovementType.WASTE,
                                   request.quantity().negate(),
                                   userId);
        movement.setWasteReason(request.wasteReason());
        movement.setReason(request.reason());

        applyMovement(movement, ErrorCode.WASTE_EXCEEDS_STOCK);

        return StockMovementView.from(movement);
    }

    /** El unico movimiento que admite signo en la peticion. Un ajuste de cero no es un ajuste. */
    public StockMovementView registerAdjustment(RegisterStockAdjustmentDTO request, Long userId)
    {
        if (request.quantity().signum() == 0)
        {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                        "El ajuste tiene que ser distinto de cero.");
        }

        var movement = newMovement(findForUpdate(request.supplyId()),
                                   MovementType.ADJUSTMENT,
                                   request.quantity(),
                                   userId);
        movement.setReason(request.reason());

        applyMovement(movement, ErrorCode.INSUFFICIENT_STOCK);

        return StockMovementView.from(movement);
    }

    // --- El unico escritor de current_stock ---------------------------------

    /**
     * Inserta el movimiento y mueve el saldo en la misma transaccion. Es el unico punto
     * del sistema que escribe supply.current_stock; cualquier otra escritura rompe la
     * reconciliacion. El codigo de error lo pone quien llama, porque un faltante significa
     * cosas distintas segun venga de una venta o de una merma.
     */
    private void applyMovement(StockMovement movement, ErrorCode shortfall)
    {
        var supply   = movement.getSupply();
        var newStock = supply.getCurrentStock().add(movement.getQuantity());

        if (newStock.signum() < 0)
        {
            throw new BusinessException(shortfall,
                    "No hay suficiente " + supply.getName() + ": el saldo es "
                    + supply.getCurrentStock() + " " + supply.getMeasureUnit()
                    + " y se pidieron " + movement.getQuantity().abs() + ".");
        }

        supply.setCurrentStock(newStock);
        stockMovementRepository.save(movement);
    }

    private StockMovement newMovement(Supply supply, MovementType type,
                                      BigDecimal signedQuantity, Long userId)
    {
        var movement = new StockMovement();
        movement.setSupply(supply);
        movement.setMovementType(type);
        movement.setQuantity(signedQuantity);
        movement.setUserId(userId);
        movement.setCreatedAt(LocalDateTime.now());

        return movement;
    }

    /** Una entrada de hoy conserva la hora, para que el kardex del dia quede ordenado. */
    private LocalDateTime timestampFor(LocalDate entryDate)
    {
        return entryDate.isEqual(LocalDate.now())
                ? LocalDateTime.now()
                : entryDate.atStartOfDay();
    }

    private Supply findForUpdate(Long supplyId)
    {
        return supplyRepository.findByIdForUpdate(supplyId)
                .orElseThrow(() -> supplyNotFound(supplyId));
    }

    private Supply findOrFail(Long supplyId)
    {
        return supplyRepository.findById(supplyId)
                .orElseThrow(() -> supplyNotFound(supplyId));
    }

    private NotFoundException supplyNotFound(Long supplyId)
    {
        return new NotFoundException(ErrorCode.SUPPLY_NOT_FOUND,
                                     "No existe el insumo " + supplyId + ".");
    }
}
