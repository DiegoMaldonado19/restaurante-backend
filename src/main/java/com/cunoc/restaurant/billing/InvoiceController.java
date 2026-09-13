package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.InvoiceView;
import com.cunoc.restaurant.billing.dto.RateServiceDTO;
import com.cunoc.restaurant.billing.dto.ServiceRatingView;
import com.cunoc.restaurant.billing.dto.VoidInvoiceDTO;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Facturación", description = "Historial, detalle, calificación y anulación de facturas")
public class InvoiceController
{
    private final BillingService billingService;

    @GetMapping
    @Operation(summary = "Historial de facturas con filtros opcionales por mesero, cliente y rango de fechas")
    public Page<InvoiceView> search(
            @RequestParam(required = false) Long waiterId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable)
    {
        return billingService.search(waiterId, customerId, from, to, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Comprobante completo: platillos, subtotal, descuentos, impuestos y propina")
    public InvoiceView findById(@PathVariable Long id)
    {
        return billingService.findInvoiceById(id);
    }

    @PostMapping("/{id}/service-ratings")
    @Operation(summary = "Califica el servicio de una factura ya emitida (1 a 5), una sola vez por factura")
    @ApiResponse(responseCode = "409", description = "RATING_ALREADY_SUBMITTED")
    public ResponseEntity<ServiceRatingView> rateService(@PathVariable Long id, @Valid @RequestBody RateServiceDTO request)
    {
        var rating = billingService.rateService(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    @PostMapping("/{id}/voids")
    @Operation(summary = "Anula la factura con motivo. El correlativo se conserva, marcado como anulado")
    @ApiResponse(responseCode = "409", description = "INVOICE_ALREADY_VOIDED")
    public InvoiceView voidInvoice(@PathVariable Long id, @Valid @RequestBody VoidInvoiceDTO request)
    {
        return billingService.voidInvoice(id, request);
    }
}