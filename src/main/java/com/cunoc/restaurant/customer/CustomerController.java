package com.cunoc.restaurant.customer;

import com.cunoc.restaurant.customer.dto.CreateCustomerDTO;
import com.cunoc.restaurant.customer.dto.CustomerDetailView;
import com.cunoc.restaurant.customer.dto.CustomerView;
import com.cunoc.restaurant.customer.dto.LoyaltyTransactionView;
import com.cunoc.restaurant.customer.dto.UpdateCustomerDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Clientes", description = "Clientes del programa de fidelizacion y su saldo de puntos")
public class CustomerController
{
    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Clientes del programa",
               description = "phone busca por coincidencia exacta: es lo que el cajero teclea en el "
                           + "mostrador. search es texto libre sobre nombre y telefono.")
    @ApiResponse(responseCode = "200", description = "Pagina de clientes")
    public PagedModel<CustomerView> findAll(@RequestParam(required = false) String   phone,
                                            @RequestParam(required = false) String   search,
                                            @ParameterObject                Pageable pageable)
    {
        return new PagedModel<>(customerService.search(phone, search, pageable));
    }

    @PostMapping
    @Operation(summary = "Alta rapida de cliente: nombre y telefono")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @ApiResponse(responseCode = "409", description = "CUSTOMER_PHONE_TAKEN")
    public ResponseEntity<CustomerView> create(@Valid @RequestBody CreateCustomerDTO request)
    {
        var customer = customerService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/customers/" + customer.customerId()))
                .body(customer);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha del cliente con sus puntos disponibles y sus visitas")
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND")
    public CustomerDetailView findById(@PathVariable Long id)
    {
        return customerService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Corrige el nombre o el telefono del cliente")
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CUSTOMER_PHONE_TAKEN")
    public CustomerView update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerDTO request)
    {
        return customerService.update(id, request);
    }

    @GetMapping("/{id}/loyalty-transactions")
    @Operation(summary = "Historial de puntos otorgados y redimidos, con fecha y factura",
               description = "No hay endpoints para mover puntos: acreditar y redimir ocurren dentro "
                           + "de POST /invoices, en la misma transaccion que la venta.")
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND")
    public PagedModel<LoyaltyTransactionView> findLoyaltyTransactions(@PathVariable    Long     id,
                                                                      @ParameterObject Pageable pageable)
    {
        return new PagedModel<>(customerService.findLoyaltyTransactions(id, pageable));
    }
}
