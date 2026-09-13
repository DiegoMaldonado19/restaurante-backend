package com.cunoc.restaurant.cashbox;

import com.cunoc.restaurant.cashbox.dto.CashShiftView;
import com.cunoc.restaurant.cashbox.dto.CashMovementView;
import com.cunoc.restaurant.cashbox.dto.OpenCashShiftDTO;
import com.cunoc.restaurant.cashbox.dto.CloseCashShiftDTO;
import com.cunoc.restaurant.cashbox.model.CashShiftStatus;
import com.cunoc.restaurant.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cash-shifts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Caja", description = "Turnos de caja y cuadre")
public class CashShiftController
{
    private final CashShiftService cashShiftService;

    @GetMapping
    @Operation(summary = "Lista turnos. El cajero solo ve los suyos; el admin ve todos")
    public List<CashShiftView> findAll(@RequestParam(required = false) CashShiftStatus status)
    {
        Long cashierId = isAdmin() ? null : CurrentUser.id();

        return cashShiftService.findAll(cashierId, status);
    }

    @PostMapping
    @Operation(summary = "Abre el turno de caja del cajero autenticado")
    @ApiResponse(responseCode = "201", description = "Turno abierto")
    @ApiResponse(responseCode = "409", description = "CASH_SHIFT_ALREADY_OPEN")
    public ResponseEntity<CashShiftView> open(@Valid @RequestBody OpenCashShiftDTO request)
    {
        var shift = cashShiftService.openShift(CurrentUser.id(), request);

        return ResponseEntity
                .created(URI.create("/api/v1/cash-shifts/" + shift.cashShiftId()))
                .body(shift);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del turno con sus totales")
    public CashShiftView findById(@PathVariable Long id)
    {
        return cashShiftService.findById(id);
    }

    @GetMapping("/{id}/movements")
    @Operation(summary = "Historial de movimientos del turno")
    public List<CashMovementView> findMovements(@PathVariable Long id)
    {
        return cashShiftService.findMovements(id);
    }

    @PostMapping("/{id}/closings")
    @Operation(summary = "Cierra el turno con cuadre de caja")
    @ApiResponse(responseCode = "404", description = "CASH_SHIFT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CASH_SHIFT_ALREADY_CLOSED")
    public CashShiftView close(@PathVariable Long id, @Valid @RequestBody CloseCashShiftDTO request)
    {
        return cashShiftService.closeShift(CurrentUser.id(), id, request);
    }

    private boolean isAdmin()
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
