package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.BillPreviewView;
import com.cunoc.restaurant.billing.dto.IssueInvoiceDTO;
import com.cunoc.restaurant.billing.dto.InvoiceView;
import com.cunoc.restaurant.billing.dto.RateServiceDTO;
import com.cunoc.restaurant.billing.dto.ServiceRatingView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}/invoices")
    @Operation(summary = "Factura la cuenta. Requiere caja abierta y que todos los items esten entregados o cancelados")
    @ApiResponse(responseCode = "409", description = "CASH_SHIFT_NOT_OPEN | ACCOUNT_HAS_PENDING_ORDERS | ACCOUNT_ALREADY_INVOICED")
    public ResponseEntity<InvoiceView> issueInvoice(@PathVariable Long id, @Valid @RequestBody IssueInvoiceDTO request)
    {
        var invoice = billingService.issueInvoice(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PostMapping("/{accountId}/invoices/{invoiceId}/ratings")
    @Operation(summary = "Califica el servicio de una factura ya emitida (1 a 5), una sola vez por factura")
    @ApiResponse(responseCode = "409", description = "RATING_ALREADY_SUBMITTED")
    public ResponseEntity<ServiceRatingView> rateService(
            @PathVariable Long accountId, @PathVariable Long invoiceId,
            @Valid @RequestBody RateServiceDTO request)
    {
        var rating = billingService.rateService(invoiceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }
}
