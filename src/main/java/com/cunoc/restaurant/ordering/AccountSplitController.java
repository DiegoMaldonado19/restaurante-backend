package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sub-cuentas", description = "Gestión de sub-cuentas (AccountSplit).")
@RestController
@RequestMapping("/api/v1/account-splits")
@RequiredArgsConstructor
@Validated
public class AccountSplitController
{
    private final TableAccountService accountService;

    @DeleteMapping("/{splitId}")
    @Operation(summary = "Eliminar sub-cuenta", description = "Elimina una sub-cuenta si no ha sido facturada.")
    @ApiResponse(responseCode = "204", description = "Sub-cuenta eliminada")
    @ApiResponse(responseCode = "404", description = "Sub-cuenta no encontrada")
    @ApiResponse(responseCode = "409", description = "Sub-cuenta ya facturada")
    public ResponseEntity<Void> deleteSplit(@PathVariable Long splitId)
    {
        accountService.deleteSplit(splitId);
        return ResponseEntity.noContent().build();
    }
}
