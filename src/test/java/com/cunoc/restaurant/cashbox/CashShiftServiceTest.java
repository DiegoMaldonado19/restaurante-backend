package com.cunoc.restaurant.cashbox;

import com.cunoc.restaurant.cashbox.dto.CloseCashShiftDTO;
import com.cunoc.restaurant.cashbox.dto.OpenCashShiftDTO;
import com.cunoc.restaurant.cashbox.model.CashMovement;
import com.cunoc.restaurant.cashbox.model.CashShift;
import com.cunoc.restaurant.cashbox.model.CashShiftStatus;
import com.cunoc.restaurant.cashbox.model.MovementType;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de CashShiftService: abrir turno, turno ya abierto, y cierre con o sin diferencia.
 */
class CashShiftServiceTest
{
    private static final Long CASHIER_ID  = 1L;
    private static final Long SHIFT_ID    = 10L;

    private final CashShiftRepository    cashShiftRepository    = mock(CashShiftRepository.class);
    private final CashMovementRepository cashMovementRepository = mock(CashMovementRepository.class);

    private final CashShiftService cashShiftService =
            new CashShiftService(cashShiftRepository, cashMovementRepository);

    private CashShift shift;

    @BeforeEach
    void setUp()
    {
        shift = new CashShift();
        shift.setCashShiftId(SHIFT_ID);
        shift.setCashierId(CASHIER_ID);
        shift.setOpeningBalance(BigDecimal.valueOf(500));
        shift.setStatus(CashShiftStatus.OPEN);

        when(cashShiftRepository.save(any(CashShift.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- Abrir turno -----------------------------------------------------

    @Test
    void abrirTurnoEsValidoCuandoNoHayUnoAbierto()
    {
        when(cashShiftRepository.findByCashierIdAndStatus(CASHIER_ID, CashShiftStatus.OPEN))
                .thenReturn(Optional.empty());

        var result = cashShiftService.openShift(CASHIER_ID, new OpenCashShiftDTO(BigDecimal.valueOf(500)));

        assertThat(result.status()).isEqualTo(CashShiftStatus.OPEN.name());
        assertThat(result.openingBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void abrirTurnoConUnoYaAbiertoEsInvalido()
    {
        when(cashShiftRepository.findByCashierIdAndStatus(CASHIER_ID, CashShiftStatus.OPEN))
                .thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> cashShiftService.openShift(CASHIER_ID, new OpenCashShiftDTO(BigDecimal.valueOf(500))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CASH_SHIFT_ALREADY_OPEN);
    }

    /** openShift siembra este movimiento, asi que el turno real siempre lo trae. */
    private CashMovement apertura(BigDecimal amount)
    {
        var movement = new CashMovement();
        movement.setMovementType(MovementType.OPENING_BALANCE);
        movement.setAmount(amount);
        return movement;
    }

    @Test
    void abrirTurnoRegistraElSaldoInicialComoMovimiento()
    {
        when(cashShiftRepository.findByCashierIdAndStatus(CASHIER_ID, CashShiftStatus.OPEN))
                .thenReturn(Optional.empty());

        cashShiftService.openShift(CASHIER_ID, new OpenCashShiftDTO(BigDecimal.valueOf(500)));

        var captor = org.mockito.ArgumentCaptor.forClass(CashMovement.class);
        org.mockito.Mockito.verify(cashMovementRepository).save(captor.capture());

        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.OPENING_BALANCE);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    // --- Cerrar turno ------------------------------------------------------

    @Test
    void cerrarTurnoConCuadreExactoEsValido()
    {
        when(cashShiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(shift));
        when(cashMovementRepository.findByCashShiftId(SHIFT_ID))
                .thenReturn(List.of(apertura(BigDecimal.valueOf(500))));

        var result = cashShiftService.closeShift(CASHIER_ID, SHIFT_ID, new CloseCashShiftDTO(BigDecimal.valueOf(500)));

        assertThat(result.status()).isEqualTo(CashShiftStatus.CLOSED.name());
        assertThat(result.difference()).isEqualByComparingTo(BigDecimal.ZERO);

        // El fondo inicial se cuenta una sola vez, no dos.
        assertThat(result.expectedCash()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void cerrarTurnoConFaltanteCalculaLaDiferenciaNegativa()
    {
        when(cashShiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(shift));
        when(cashMovementRepository.findByCashShiftId(SHIFT_ID))
                .thenReturn(List.of(apertura(BigDecimal.valueOf(500))));

        // Se esperaban 500 (solo el fondo inicial), pero se contaron 480: faltan 20.
        var result = cashShiftService.closeShift(CASHIER_ID, SHIFT_ID, new CloseCashShiftDTO(BigDecimal.valueOf(480)));

        assertThat(result.expectedCash()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.difference()).isEqualByComparingTo(BigDecimal.valueOf(-20));
    }

    @Test
    void cerrarTurnoConSobranteSumaVentasEnEfectivo()
    {
        var venta = new CashMovement();
        venta.setMovementType(MovementType.CASH_SALE);
        venta.setAmount(BigDecimal.valueOf(100));

        when(cashShiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(shift));
        when(cashMovementRepository.findByCashShiftId(SHIFT_ID))
                .thenReturn(List.of(apertura(BigDecimal.valueOf(500)), venta));

        // Esperado: 500 (fondo) + 100 (venta) = 600. Se contaron 610: sobran 10.
        var result = cashShiftService.closeShift(CASHIER_ID, SHIFT_ID, new CloseCashShiftDTO(BigDecimal.valueOf(610)));

        assertThat(result.expectedCash()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(result.difference()).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void cerrarTurnoYaCerradoEsInvalido()
    {
        shift.setStatus(CashShiftStatus.CLOSED);
        when(cashShiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> cashShiftService.closeShift(CASHIER_ID, SHIFT_ID, new CloseCashShiftDTO(BigDecimal.valueOf(500))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CASH_SHIFT_ALREADY_CLOSED);
    }

    @Test
    void cerrarTurnoDeOtroCajeroEsInvalido()
    {
        when(cashShiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(shift));

        Long otroCajero = 999L;

        assertThatThrownBy(() -> cashShiftService.closeShift(otroCajero, SHIFT_ID, new CloseCashShiftDTO(BigDecimal.valueOf(500))))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CASH_SHIFT_NOT_FOUND);
    }

    @Test
    void cerrarTurnoInexistenteEsInvalido()
    {
        when(cashShiftRepository.findById(SHIFT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cashShiftService.closeShift(CASHIER_ID, SHIFT_ID, new CloseCashShiftDTO(BigDecimal.valueOf(500))))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CASH_SHIFT_NOT_FOUND);
    }

    // --- requireOpenShift ----------------------------------------------------

    @Test
    void requireOpenShiftRegresaElTurnoSiExiste()
    {
        when(cashShiftRepository.findByCashierIdAndStatus(CASHIER_ID, CashShiftStatus.OPEN))
                .thenReturn(Optional.of(shift));

        var result = cashShiftService.requireOpenShift(CASHIER_ID);

        assertThat(result.getCashShiftId()).isEqualTo(SHIFT_ID);
    }

    @Test
    void requireOpenShiftFallaSiNoHayTurnoAbierto()
    {
        when(cashShiftRepository.findByCashierIdAndStatus(CASHIER_ID, CashShiftStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cashShiftService.requireOpenShift(CASHIER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CASH_SHIFT_NOT_OPEN);
    }
}