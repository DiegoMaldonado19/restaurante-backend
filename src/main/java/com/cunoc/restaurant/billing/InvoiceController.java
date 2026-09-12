package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.InvoiceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Facturación", description = "Historial de facturas")
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
}
