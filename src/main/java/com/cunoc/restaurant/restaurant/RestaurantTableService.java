package com.cunoc.restaurant.restaurant;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.restaurant.dto.CreateTableDTO;
import com.cunoc.restaurant.restaurant.dto.RestaurantTableView;
import com.cunoc.restaurant.restaurant.dto.UpdateTableDTO;
import com.cunoc.restaurant.restaurant.dto.UpdateTableStatusDTO;
import com.cunoc.restaurant.restaurant.model.RestaurantTable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mesas del salon. transitionTo() es el UNICO metodo del sistema que escribe
 * restaurant_table.status (02 · §12.2): dining, ordering y billing lo llaman, y el
 * PATCH manual tambien pasa por aqui para no duplicar la matriz. El bloqueo de fila
 * hace la transicion atomica bajo concurrencia.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantTableService
{
    private final RestaurantTableRepository tableRepository;

    // --- Contrato con los otros modulos -------------------------------------

    /**
     * Unico escritor de restaurant_table.status. La matriz vive aqui y en ningun
     * otro lado. Publico porque lo invocan dining (reservar/sentar), ordering
     * (abrir cuenta, transferir, pedir la cuenta, anular) y billing (liberar al cobrar).
     */
    public RestaurantTableView transitionTo(Long tableId, TableStatus target)
    {
        var table = findForUpdate(tableId);

        if (!canTransition(table.getStatus(), target))
        {
            throw new BusinessException(ErrorCode.INVALID_TABLE_TRANSITION,
                    "No se puede pasar la mesa " + table.getTableNumber()
                    + " de " + table.getStatus() + " a " + target + ".");
        }

        table.setStatus(target);

        return RestaurantTableView.from(table);
    }

    // --- Los ocho endpoints -------------------------------------------------

    @Transactional(readOnly = true)
    public Page<RestaurantTableView> search(TableZone zone, TableStatus status,
                                            Integer minCapacity, Pageable pageable)
    {
        return tableRepository.search(zone, status, minCapacity, pageable)
                .map(RestaurantTableView::from);
    }

    public RestaurantTableView create(CreateTableDTO request)
    {
        if (tableRepository.existsByTableNumber(request.tableNumber()))
        {
            throw new BusinessException(ErrorCode.TABLE_NUMBER_TAKEN,
                    "Ya existe la mesa " + request.tableNumber() + ".");
        }

        var table = new RestaurantTable();
        table.setTableNumber(request.tableNumber());
        table.setCapacity(request.capacity());
        table.setZone(request.zone());
        table.setStatus(request.status());
        table.setActive(true);

        return RestaurantTableView.from(tableRepository.save(table));
    }

    @Transactional(readOnly = true)
    public RestaurantTableView findById(Long tableId)
    {
        return RestaurantTableView.from(findOrFail(tableId));
    }

    /** No toca el status: el estado solo se mueve por transitionTo(). */
    public RestaurantTableView update(Long tableId, UpdateTableDTO request)
    {
        var table = findOrFail(tableId);

        if (tableRepository.existsByTableNumberAndRestaurantTableIdNot(request.tableNumber(), tableId))
        {
            throw new BusinessException(ErrorCode.TABLE_NUMBER_TAKEN,
                    "Ya existe la mesa " + request.tableNumber() + ".");
        }

        table.setTableNumber(request.tableNumber());
        table.setCapacity(request.capacity());
        table.setZone(request.zone());

        return RestaurantTableView.from(table);
    }

    /**
     * Transicion manual (PATCH): la valvula de recuperacion cuando una mesa queda mal.
     * Pasa por la misma matriz que transitionTo(); no existe otro camino para el status.
     */
    public RestaurantTableView changeStatus(Long tableId, UpdateTableStatusDTO request)
    {
        return transitionTo(tableId, request.status());
    }

    /**
     * Baja logica (DELETE). Solo se permite en mesa libre: dar de baja una mesa con
     * cuenta abierta o por cobrar dejaria cuentas huerfanas. Decision del equipo
     * (ANALISIS §11): la opcion estricta, TABLA_NOT_FREE como 409.
     */
    public void deactivate(Long tableId)
    {
        var table = findOrFail(tableId);

        if (table.getStatus() != TableStatus.FREE)
        {
            throw new BusinessException(ErrorCode.TABLE_NOT_FREE,
                    "No se puede dar de baja la mesa " + table.getTableNumber()
                    + ": su estado es " + table.getStatus() + ".");
        }

        table.setActive(false);
    }

    // --- La matriz de transiciones (03 · §1.4) ------------------------------

    private boolean canTransition(TableStatus from, TableStatus to)
    {
        return switch (from)
        {
            case FREE           -> to == TableStatus.RESERVED || to == TableStatus.OCCUPIED;
            case RESERVED       -> to == TableStatus.FREE     || to == TableStatus.OCCUPIED;
            case OCCUPIED       -> to == TableStatus.FREE     || to == TableStatus.OCCUPIED
                                || to == TableStatus.BILL_REQUESTED;
            case BILL_REQUESTED -> to == TableStatus.FREE     || to == TableStatus.OCCUPIED;
        };
    }

    // --- Interno ------------------------------------------------------------

    private RestaurantTable findForUpdate(Long tableId)
    {
        return tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> tableNotFound(tableId));
    }

    private RestaurantTable findOrFail(Long tableId)
    {
        return tableRepository.findById(tableId)
                .orElseThrow(() -> tableNotFound(tableId));
    }

    private NotFoundException tableNotFound(Long tableId)
    {
        return new NotFoundException(ErrorCode.TABLE_NOT_FOUND,
                                     "No existe la mesa " + tableId + ".");
    }
}