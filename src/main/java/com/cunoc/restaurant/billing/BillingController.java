package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.BillPreviewView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Facturación", description = "Precuenta y comprobantes")
public class BillingController
{
    private final BillingService billingService;

    @GetMapping("/{id}/bill-preview")
    @Operation(summary = "Precuenta: subtotal, impuesto, propina sugerida y desglose por sub-cuenta. No emite nada ni exige caja abierta")
    public BillPreviewView billPreview(@PathVariable Long id)
    {
        return billingService.billPreview(id);
    }
}
