package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.common.security.CurrentUser;
import com.cunoc.restaurant.ordering.dto.*;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.AccountSplit;
import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.TableAccount;
import com.cunoc.restaurant.restaurant.RestaurantTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TableAccountService
{
    private final TableAccountRepository accountRepository;
    private final AccountSplitRepository splitRepository;
    private final OrderTicketRepository ticketRepository;
    private final OrderItemRepository itemRepository;
    private final RestaurantTableService tableService;

    // --- Contrato público con los controladores ----------------------------

    @Transactional(readOnly = true)
    public Page<TableAccountView> search(AccountStatus status, Long tableId,
                                         Long waiterId, LocalDateTime from, LocalDateTime to,
                                         Pageable pageable)
    {
        return TableAccountView.page(accountRepository.search(status, tableId, waiterId, from, to, pageable));
    }

    public TableAccountView open(OpenAccountDTO request)
    {
        Long waiterId = CurrentUser.id();
        var table = tableService.findById(request.tableId());

        if (table.status() != TableStatus.FREE)
        {
            if (table.status() == TableStatus.RESERVED)
                throw new BusinessException(ErrorCode.TABLE_RESERVED,
                        "La mesa " + table.restaurantTableId() + " está reservada. Se sugiere asignar la reserva existente.");
            if (table.status() == TableStatus.OCCUPIED || table.status() == TableStatus.BILL_REQUESTED)
                throw new BusinessException(ErrorCode.TABLE_NOT_FREE,
                        "La mesa " + table.restaurantTableId() + " no está libre. Estatus actual: " + table.status() + ".");
            throw new BusinessException(ErrorCode.TABLE_NOT_FOUND,
                    "La mesa " + request.tableId() + " no existe.");
        }

        if (request.guestCount() > table.capacity())
            throw new BusinessException(ErrorCode.TABLE_CAPACITY_EXCEEDED,
                    "La mesa " + table.restaurantTableId() + " tiene capacidad para " + table.capacity() + " comensales, se solicitaron " + request.guestCount() + ".");

        if (accountRepository.findByRestaurantTableIdAndStatusIn(request.tableId(),
                        Set.of(AccountStatus.OPEN, AccountStatus.BILL_REQUESTED)).isPresent())
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_OPEN,
                    "Ya existe una cuenta abierta o lista para cobro en la mesa " + table.restaurantTableId() + ".");

        tableService.transitionTo(table.restaurantTableId(), TableStatus.OCCUPIED);

        var account = new TableAccount();
        account.setRestaurantTableId(table.restaurantTableId());
        account.setWaiterId(waiterId);
        account.setGuestCount(request.guestCount());
        account.setStatus(AccountStatus.OPEN);
        account.setOpenedAt(LocalDateTime.now());

        accountRepository.save(account);
        log.info("Cuenta {} abierta en mesa {}. Mesero: {}", account.getTableAccountId(), table.restaurantTableId(), waiterId);

        return findById(account.getTableAccountId());
    }

    @Transactional(readOnly = true)
    public TableAccountView findById(Long accountId)
    {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "No existe la cuenta " + accountId + "."));
        return TableAccountView.from(account);
    }

    public TableAccountView transfer(Long accountId, TransferAccountDTO request)
    {
        var account = findAccountForUpdate(accountId);
        validateAccountOpen(account);

        var targetTable = tableService.findById(request.targetTableId());

        if (targetTable.status() == TableStatus.RESERVED)
            throw new BusinessException(ErrorCode.TABLE_RESERVED,
                    "La mesa destino " + targetTable.restaurantTableId() + " está reservada.");
        if (targetTable.status() != TableStatus.FREE)
            throw new BusinessException(ErrorCode.TABLE_NOT_FREE,
                    "La mesa destino " + targetTable.restaurantTableId() + " no está libre.");

        tableService.transitionTo(account.getRestaurantTableId(), TableStatus.FREE);
        tableService.transitionTo(targetTable.restaurantTableId(), TableStatus.OCCUPIED);

        account.setRestaurantTableId(targetTable.restaurantTableId());
        accountRepository.save(account);
        log.info("Cuenta {} transferida de mesa {} a mesa {}", accountId, account.getRestaurantTableId(), targetTable.restaurantTableId());

        return findById(accountId);
    }

    public TableAccountView merge(Long accountId, MergeAccountDTO request)
    {
        var targetAccount = findAccountForUpdate(accountId);
        var sourceAccount = accountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "No existe la cuenta origen " + request.sourceAccountId() + "."));

        if (!targetAccount.getStatus().equals(AccountStatus.OPEN) || !sourceAccount.getStatus().equals(AccountStatus.OPEN))
            throw new BusinessException(ErrorCode.ACCOUNT_MERGE_INVALID,
                    "Ambas cuentas deben estar en estado OPEN para poder fusionarse.");

        if (targetAccount.getTableAccountId().equals(sourceAccount.getTableAccountId()))
            throw new BusinessException(ErrorCode.ACCOUNT_MERGE_INVALID,
                    "No se puede fusionar una cuenta consigo misma.");

        // Transferir tickets de origen a destino
        sourceAccount.getOrderTickets().forEach(ticket -> ticket.setAccount(targetAccount));
        ticketRepository.saveAll(sourceAccount.getOrderTickets());

        sourceAccount.setStatus(AccountStatus.MERGED);
        sourceAccount.setMergedInto(targetAccount);
        accountRepository.save(sourceAccount);

        tableService.transitionTo(sourceAccount.getRestaurantTableId(), TableStatus.FREE);
        log.info("Cuenta {} fusionada en cuenta {}", sourceAccount.getTableAccountId(), targetAccount.getTableAccountId());

        return findById(accountId);
    }

    public List<AccountSplitView> split(Long accountId, SplitAccountDTO request)
    {
        var account = findAccountForUpdate(accountId);
        validateAccountOpen(account);

        if (request.mode() == com.cunoc.restaurant.ordering.model.SplitMode.BY_PERSON)
        {
            if (request.personCount() == null || request.personCount() < 2 || request.personCount() > 10)
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "El número de personas para la división por persona debe estar entre 2 y 10.");

            BigDecimal totalAmount = splitRepository.findByAccountTableAccountId(accountId)
                    .stream()
                    .map(AccountSplit::getShareAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal shareAmount = totalAmount.divide(BigDecimal.valueOf(request.personCount()), 2, RoundingMode.HALF_UP);

            List<AccountSplit> newSplits = new ArrayList<>();
            for (int i = 1; i <= request.personCount(); i++)
            {
                var split = new AccountSplit();
                split.setAccount(account);
                split.setMode(com.cunoc.restaurant.ordering.model.SplitMode.BY_PERSON);
                split.setLabel("Persona " + i);
                split.setShareAmount(shareAmount);
                split.setCreatedAt(LocalDateTime.now());
                newSplits.add(split);
            }
            splitRepository.saveAll(newSplits);
            log.info("Cuenta {} dividida en {} partes iguales. Monto por parte: {}", accountId, request.personCount(), shareAmount);
            return newSplits.stream().map(AccountSplitView::from).collect(Collectors.toList());
        }
        else // BY_ITEM
        {
            if (request.items() == null || request.items().isEmpty())
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Se requieren líneas de ítems para la división por item.");

            List<AccountSplit> newSplits = request.items().stream()
                    .map(line ->
                    {
                        var split = new AccountSplit();
                        split.setAccount(account);
                        split.setMode(com.cunoc.restaurant.ordering.model.SplitMode.BY_ITEM);
                        split.setLabel("Split#" + System.currentTimeMillis());
                        split.setShareAmount(BigDecimal.ZERO);
                        split.setCreatedAt(LocalDateTime.now());
                        return split;
                    })
                    .collect(Collectors.toList());

            splitRepository.saveAll(newSplits);

            List<Long> splitIds = new ArrayList<>();
            for (AccountSplit split : newSplits)
            {
                splitIds.add(split.getAccountSplitId());
            }

            for (int i = 0; i < request.items().size(); i++)
            {
                var line = request.items().get(i);
                var item = itemRepository.findById(line.orderItemId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_ITEM_NOT_FOUND,
                                "No existe el ítem " + line.orderItemId() + "."));

                var split = splitRepository.findById(line.accountSplitId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                                "No existe la sub-cuenta " + line.accountSplitId() + "."));

                if (!splitIds.contains(line.accountSplitId()))
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La sub-cuenta en la línea " + i + " no pertenece a esta división.");

                if (item.getSplit() != null)
                    throw new BusinessException(ErrorCode.SPLIT_ITEMS_MISMATCH,
                            "El ítem " + item.getOrderItemId() + " ya estaba asignado a otra sub-cuenta.");

                item.setSplit(split);
                itemRepository.save(item);
            }

            log.info("Cuenta {} dividida en {} sub-cuentas por ítems.", accountId, newSplits.size());
            return newSplits.stream().map(AccountSplitView::from).collect(Collectors.toList());
        }
    }

    public void deleteSplit(Long splitId)
    {
        var split = splitRepository.findById(splitId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "No existe la sub-cuenta " + splitId + "."));

        var account = split.getAccount();
        if (account.getStatus() != AccountStatus.OPEN && account.getStatus() != AccountStatus.BILL_REQUESTED)
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_OPEN,
                    "No se puede eliminar una sub-cuenta de una cuenta que no está abierta o lista para cobro.");

        // Devolver ítems asignados al pool
        List<OrderItem> itemsToUnassign = itemRepository.findBySplitAccountSplitId(splitId);

        itemsToUnassign.forEach(item ->
        {
            item.setSplit(null);
            itemRepository.save(item);
        });

        // Eliminar la sub-cuenta (si fue facturada, DataIntegrityViolationException -> 409)
        try
        {
            splitRepository.delete(split);
            log.info("Sub-cuenta {} eliminada.", splitId);
        }
        catch (DataIntegrityViolationException e)
        {
            throw new BusinessException(ErrorCode.SPLIT_ALREADY_INVOICED,
                    "La sub-cuenta " + splitId + " ya fue facturada y no puede ser eliminada.");
        }
    }

    public TableAccountView requestBill(Long accountId)
    {
        var account = findAccountForUpdate(accountId);
        validateAccountOpen(account);

        account.setStatus(AccountStatus.BILL_REQUESTED);
        accountRepository.save(account);

        tableService.transitionTo(account.getRestaurantTableId(), TableStatus.BILL_REQUESTED);
        log.info("Cuenta {} marcada como lista para cobro.", accountId);

        return findById(accountId);
    }

    public TableAccountView cancel(Long accountId, CancelAccountDTO request)
    {
        var account = findAccountForUpdate(accountId);

        if (account.getStatus() != AccountStatus.OPEN && account.getStatus() != AccountStatus.BILL_REQUESTED)
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_OPEN,
                    "Solo se pueden anular cuentas abiertas o listas para cobro. Estado actual: " + account.getStatus() + ".");

        account.setStatus(AccountStatus.CANCELLED);
        account.setCancellationReason(request.reason());
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        tableService.transitionTo(account.getRestaurantTableId(), TableStatus.FREE);
        log.info("Cuenta {} anulada. Motivo: {}", accountId, request.reason());

        return findById(accountId);
    }

    // --- Validaciones internas ----------------------------------------------

    @Transactional(readOnly = true)
    private TableAccount findAccountForUpdate(Long accountId)
    {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "No existe la cuenta " + accountId + "."));
    }

    private void validateAccountOpen(TableAccount account)
    {
        if (account.getStatus() != AccountStatus.OPEN)
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_OPEN,
                    "La cuenta " + account.getTableAccountId() + " no está abierta. Estado actual: " + account.getStatus() + ".");
    }
}
