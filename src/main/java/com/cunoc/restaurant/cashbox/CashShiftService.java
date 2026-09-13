package com.cunoc.restaurant.cashbox;

import com.cunoc.restaurant.cashbox.dto.CashShiftView;
import com.cunoc.restaurant.cashbox.dto.CashMovementView;
import com.cunoc.restaurant.cashbox.dto.OpenCashShiftDTO;
import com.cunoc.restaurant.cashbox.dto.CloseCashShiftDTO;
import com.cunoc.restaurant.cashbox.model.CashShift;
import com.cunoc.restaurant.cashbox.model.CashMovement;
import com.cunoc.restaurant.cashbox.model.CashShiftStatus;
import com.cunoc.restaurant.cashbox.model.MovementType;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CashShiftService
{
    private final CashShiftRepository    cashShiftRepository;
    private final CashMovementRepository cashMovementRepository;

    public CashShift requireOpenShift(Long cashierId)
    {
        return cashShiftRepository.findByCashierIdAndStatus(cashierId, CashShiftStatus.OPEN)
                .orElseThrow(() -> new BusinessException(ErrorCode.CASH_SHIFT_NOT_OPEN));
    }

    public CashShiftView openShift(Long cashierId, OpenCashShiftDTO request)
    {
        cashShiftRepository.findByCashierIdAndStatus(cashierId, CashShiftStatus.OPEN)
                .ifPresent(shift -> { throw new BusinessException(ErrorCode.CASH_SHIFT_ALREADY_OPEN); });

        var shift = new CashShift();
        shift.setCashierId(cashierId);
        shift.setOpeningBalance(request.openingBalance());
        shift.setStatus(CashShiftStatus.OPEN);
        shift.setOpenedAt(LocalDateTime.now());
        shift = cashShiftRepository.save(shift);

        // El saldo inicial es un movimiento mas: asi aparece en el historial del turno,
        // que el enunciado pide, y el cuadre lo suma desde la misma lista que el resto.
        var opening = new CashMovement();
        opening.setCashShiftId(shift.getCashShiftId());
        opening.setMovementType(MovementType.OPENING_BALANCE);
        opening.setAmount(request.openingBalance());
        opening.setCreatedAt(LocalDateTime.now());
        cashMovementRepository.save(opening);

        return CashShiftView.from(shift);
    }

    public CashMovementView registerMovement(Long cashierId, MovementType type, BigDecimal amount, Long invoiceId)
    {
        var shift = requireOpenShift(cashierId);

        var movement = new CashMovement();
        movement.setCashShiftId(shift.getCashShiftId());
        movement.setMovementType(type);
        movement.setAmount(amount);
        movement.setInvoiceId(invoiceId);
        movement.setCreatedAt(LocalDateTime.now());

        return CashMovementView.from(cashMovementRepository.save(movement));
    }

    public CashShiftView closeShift(Long cashierId, Long shiftId, CloseCashShiftDTO request)
    {
        var shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CASH_SHIFT_NOT_FOUND));

        if (!shift.getCashierId().equals(cashierId))
        {
            throw new NotFoundException(ErrorCode.CASH_SHIFT_NOT_FOUND);
        }

        if (shift.getStatus() != CashShiftStatus.OPEN)
        {
            throw new BusinessException(ErrorCode.CASH_SHIFT_ALREADY_CLOSED);
        }

        var cashMovements = cashMovementRepository.findByCashShiftId(shift.getCashShiftId());

        var cashTypes = Set.of(MovementType.OPENING_BALANCE, MovementType.CASH_SALE, MovementType.CASH_TIP);

        // Se parte de cero: OPENING_BALANCE ya es uno de los movimientos filtrados y
        // sembrar el reduce con el saldo inicial lo contaria dos veces.
        var expectedCash = cashMovements.stream()
                .filter(m -> cashTypes.contains(m.getMovementType()))
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        shift.setExpectedCash(expectedCash);
        shift.setCountedCash(request.countedCash());
        shift.setDifference(request.countedCash().subtract(expectedCash));
        shift.setStatus(CashShiftStatus.CLOSED);
        shift.setClosedAt(LocalDateTime.now());

        return CashShiftView.from(cashShiftRepository.save(shift));
    }

    @Transactional(readOnly = true)
    public CashShiftView findById(Long id)
    {
        var shift = cashShiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CASH_SHIFT_NOT_FOUND));

        return CashShiftView.from(shift);
    }

    @Transactional(readOnly = true)
    public List<CashMovementView> findMovements(Long shiftId)
    {
        return cashMovementRepository.findByCashShiftId(shiftId).stream()
                .map(CashMovementView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashShiftView> findAll(Long cashierId, CashShiftStatus status)
    {
        List<CashShift> shifts;

        if (cashierId != null && status != null)
        {
            shifts = cashShiftRepository.findByCashierIdAndStatus(cashierId, status)
                    .map(List::of)
                    .orElse(List.of());
        }
        else if (cashierId != null)
        {
            shifts = cashShiftRepository.findByCashierId(cashierId);
        }
        else if (status != null)
        {
            shifts = cashShiftRepository.findByStatus(status);
        }
        else
        {
            shifts = cashShiftRepository.findAll();
        }

        return shifts.stream().map(CashShiftView::from).toList();
    }
}
