package com.cunoc.restaurant.dining;

import com.cunoc.restaurant.dining.dto.*;
import com.cunoc.restaurant.dining.model.WaitlistStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/waitlist-entries")
@RequiredArgsConstructor
@Tag(name = "Lista de espera", description = "Cola de walk-ins con sugerencia automatica")
public class WaitlistController
{
    private final DiningService diningService;

    @GetMapping
    @Operation(summary = "Cola en orden de llegada. fits_table_id filtra solo quienes caben en esa mesa (sugerencia automatica)")
    public List<WaitlistEntryView> search(
            @RequestParam(required = false) WaitlistStatus status,
            @RequestParam(required = false, name = "fits_table_id") Long fitsTableId)
    {
        return diningService.searchWaitlist(status, fitsTableId);
    }

    @PostMapping
    @Operation(summary = "Agrega un walk-in a la cola: nombre, telefono, personas y hora de llegada")
    public ResponseEntity<WaitlistEntryView> create(@Valid @RequestBody CreateWaitlistEntryDTO request)
    {
        var entry = diningService.createWaitlistEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @PostMapping("/{id}/seatings")
    @Operation(summary = "Sienta al cliente de la cola en una mesa compatible y abre la cuenta")
    @ApiResponse(responseCode = "409", description = "WAITLIST_TABLE_TOO_SMALL")
    public SeatingResultView seat(@PathVariable Long id, @Valid @RequestBody SeatWaitlistDTO request)
    {
        return diningService.seatWaitlistEntry(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "El cliente se canso de esperar y se fue")
    public ResponseEntity<Void> remove(@PathVariable Long id)
    {
        diningService.removeWaitlistEntry(id);
        return ResponseEntity.noContent().build();
    }
}
