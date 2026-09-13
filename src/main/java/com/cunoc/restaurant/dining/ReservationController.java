package com.cunoc.restaurant.dining;

import com.cunoc.restaurant.dining.dto.*;
import com.cunoc.restaurant.dining.model.ReservationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Reservas de mesa con bloqueo de horario")
public class ReservationController
{
    private final DiningService diningService;

    @GetMapping
    @Operation(summary = "Reservas. Filtros: date, status, table_id")
    public Page<ReservationView> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Long tableId,
            Pageable pageable)
    {
        return diningService.searchReservations(date, status, tableId, pageable);
    }

    @PostMapping
    @Operation(summary = "Crea la reserva con nombre, telefono, fecha, hora y personas; bloquea la mesa")
    @ApiResponse(responseCode = "409", description = "RESERVATION_SLOT_UNAVAILABLE | TABLE_CAPACITY_EXCEEDED")
    public ResponseEntity<ReservationView> create(@Valid @RequestBody CreateReservationDTO request)
    {
        var reservation = diningService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de la reserva")
    public ReservationView findById(@PathVariable Long id)
    {
        return diningService.findReservationById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reprograma fecha, hora, personas o mesa. Revalida disponibilidad")
    @ApiResponse(responseCode = "409", description = "RESERVATION_SLOT_UNAVAILABLE | RESERVATION_ALREADY_CLOSED")
    public ReservationView update(@PathVariable Long id, @Valid @RequestBody UpdateReservationDTO request)
    {
        return diningService.updateReservation(id, request);
    }

    @PostMapping("/{id}/seatings")
    @Operation(summary = "Sienta al cliente: pasa la mesa a OCCUPIED y abre la cuenta")
    @ApiResponse(responseCode = "409", description = "RESERVATION_NOT_DUE | RESERVATION_ALREADY_CLOSED")
    public SeatingResultView seat(@PathVariable Long id)
    {
        return diningService.seatReservation(id);
    }

    @PostMapping("/{id}/cancellations")
    @Operation(summary = "Cierra la reserva y libera la mesa")
    @ApiResponse(responseCode = "409", description = "RESERVATION_ALREADY_CLOSED")
    public ReservationView cancel(@PathVariable Long id, @Valid @RequestBody CancelReservationDTO request)
    {
        return diningService.cancelReservation(id, request);
    }
}
