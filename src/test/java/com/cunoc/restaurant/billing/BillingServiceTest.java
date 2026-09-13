package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.IssueInvoiceDTO;
import com.cunoc.restaurant.billing.dto.PaymentDTO;
import com.cunoc.restaurant.billing.model.Invoice;
import com.cunoc.restaurant.billing.model.InvoicePayment;
import com.cunoc.restaurant.billing.model.InvoiceSequence;
import com.cunoc.restaurant.billing.model.InvoiceStatus;
import com.cunoc.restaurant.billing.model.PaymentMethod;
import com.cunoc.restaurant.cashbox.CashShiftService;
import com.cunoc.restaurant.cashbox.dto.CashMovementView;
import com.cunoc.restaurant.cashbox.model.CashShift;
import com.cunoc.restaurant.cashbox.model.CashShiftStatus;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.customer.CustomerService;
import com.cunoc.restaurant.ordering.TableAccountService;
import com.cunoc.restaurant.ordering.dto.DishModifierView;
import com.cunoc.restaurant.ordering.dto.OrderItemView;
import com.cunoc.restaurant.ordering.dto.OrderTicketView;
import com.cunoc.restaurant.ordering.dto.TableAccountView;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.restaurant.RestaurantSettingService;
import com.cunoc.restaurant.restaurant.dto.RestaurantSettingView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de BillingService.issueInvoice(): caja abierta, items pendientes,
 * cuenta ya facturada, descuadre de pagos, y el flujo exitoso completo.
 */
class BillingServiceTest
{
    private static final Long CASHIER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long SHIFT_ID   = 20L;

    private final TableAccountService       tableAccountService       = mock(TableAccountService.class);
    private final CashShiftService          cashShiftService          = mock(CashShiftService.class);
    private final CustomerService           customerService           = mock(CustomerService.class);
    private final InvoiceRepository         invoiceRepository         = mock(InvoiceRepository.class);
    private final InvoiceSequenceRepository invoiceSequenceRepository = mock(InvoiceSequenceRepository.class);
    private final ServiceRatingRepository   serviceRatingRepository   = mock(ServiceRatingRepository.class);
    private final InvoicePaymentRepository  invoicePaymentRepository  = mock(InvoicePaymentRepository.class);
    private final RestaurantSettingService  settingService            = mock(RestaurantSettingService.class);

    private final BillingService billingService = new BillingService(
            tableAccountService, cashShiftService, customerService,
            invoiceRepository, invoiceSequenceRepository, serviceRatingRepository,
            invoicePaymentRepository, settingService);

    private CashShift shift;
    private InvoiceSequence sequence;

    @BeforeEach
    void setUp()
    {
        // Mock security context, igual que TableAccountServiceTest
        var jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(String.valueOf(CASHIER_ID));
        var auth = new UsernamePasswordAuthenticationToken(jwt, null,
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        shift = new CashShift();
        shift.setCashShiftId(SHIFT_ID);
        shift.setCashierId(CASHIER_ID);
        shift.setStatus(CashShiftStatus.OPEN);

        sequence = new InvoiceSequence();
        sequence.setSequenceId(1L);
        sequence.setNextNumber(1L);

        // Los mismos valores que siembra V2: 12% de impuesto, 10% de propina sugerida,
        // 1 punto por quetzal y Q0.10 por punto al redimir.
        when(settingService.get()).thenReturn(new RestaurantSettingView(
                1L, BigDecimal.valueOf(12), BigDecimal.valueOf(10),
                BigDecimal.ONE, BigDecimal.valueOf(0.10)));

        when(invoiceSequenceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sequence));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePaymentRepository.save(any(InvoicePayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cashShiftService.registerMovement(any(), any(), any(), any()))
                .thenReturn(mock(CashMovementView.class));
    }
    @AfterEach
    void limpiarContextoDeSeguridad()
    {
        // El SecurityContextHolder es estatico y surefire reutiliza la JVM: sin esto la
        // autenticacion se filtra a la siguiente clase y UserControllerSecurityTest ve un
        // token donde esperaba una peticion anonima.
        SecurityContextHolder.clearContext();
    }


    private OrderItemView deliveredItem(BigDecimal unitPrice, int quantity)
    {
        return new OrderItemView(1L, 1L, "Pollo a la plancha", List.<DishModifierView>of(), null,
                quantity, unitPrice, BigDecimal.ZERO, null, OrderItemStatus.DELIVERED,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), false, null);
    }

    private OrderItemView pendingItem()
    {
        return new OrderItemView(2L, 2L, "Ensalada", List.<DishModifierView>of(), null,
                1, BigDecimal.TEN, BigDecimal.ZERO, null, OrderItemStatus.IN_PREPARATION,
                LocalDateTime.now(), null, null, false, null);
    }

    private TableAccountView accountWith(OrderItemView... items)
    {
        var ticket = new OrderTicketView(1L, ACCOUNT_ID, 3L, LocalDateTime.now(),
                List.of(items), OrderItemStatus.DELIVERED);

        return new TableAccountView(ACCOUNT_ID, 5L, 4, null, LocalDateTime.now(), null,
                new TableAccountView.SplitsInfo(0, BigDecimal.ZERO), List.of(ticket),
                BigDecimal.ZERO, 3L, "Waiter#3");
    }

    // --- Caja abierta --------------------------------------------------------

    @Test
    void facturarSinCajaAbiertaEsInvalido()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID))
                .thenThrow(new BusinessException(ErrorCode.CASH_SHIFT_NOT_OPEN));

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(100))), null, null, null);

        assertThatThrownBy(() -> billingService.issueInvoice(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CASH_SHIFT_NOT_OPEN);
    }

    // --- Items pendientes ------------------------------------------------------

    @Test
    void facturarConItemsSinEntregarEsInvalido()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(pendingItem()));

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.TEN)), null, null, null);

        assertThatThrownBy(() -> billingService.issueInvoice(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_HAS_PENDING_ORDERS);
    }

    // --- Ya facturada ------------------------------------------------------

    @Test
    void facturarUnaCuentaYaFacturadaEsInvalido()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(55), 1)));

        var facturaExistente = new Invoice();
        facturaExistente.setInvoiceNumber(99L);
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.of(facturaExistente));

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(61.60))), null, null, null);

        assertThatThrownBy(() -> billingService.issueInvoice(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_ALREADY_INVOICED);
    }

    // --- Descuadre de pagos ------------------------------------------------------

    @Test
    void facturarConPagosQueNoCuadranConElTotalEsInvalido()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        // Subtotal 110 + 12% impuesto = 123.20, pero se paga solo 100.
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(55), 2)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(100))), null, null, null);

        assertThatThrownBy(() -> billingService.issueInvoice(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    // --- Flujo exitoso ------------------------------------------------------

    @Test
    void facturarUnaCuentaValidaCalculaLosMontosYCierraLaCuenta()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        // Dos platillos a Q55 c/u = Q110 de subtotal, +12% impuesto = Q123.20
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(55), 2)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(123.20))), null, null, null);

        var result = billingService.issueInvoice(ACCOUNT_ID, request);

        assertThat(result.subtotal()).isEqualByComparingTo(BigDecimal.valueOf(110));
        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(13.20));
        assertThat(result.total()).isEqualByComparingTo(BigDecimal.valueOf(123.20));
        assertThat(result.invoiceNumber()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(InvoiceStatus.ISSUED);

        // La cuenta se cierra y se registra el movimiento en caja
        org.mockito.Mockito.verify(tableAccountService).close(ACCOUNT_ID);
        org.mockito.Mockito.verify(cashShiftService).registerMovement(
                org.mockito.ArgumentMatchers.eq(CASHIER_ID),
                org.mockito.ArgumentMatchers.eq(com.cunoc.restaurant.cashbox.model.MovementType.CASH_SALE),
                org.mockito.ArgumentMatchers.argThat(amount -> amount.compareTo(BigDecimal.valueOf(123.20)) == 0),
                any());
    }

    @Test
    void facturarConPagoCombinadoRegistraCadaMovimientoPorSeparado()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        // Total 123.20: Q73.20 en efectivo + Q50.00 con tarjeta
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(55), 2)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        var request = new IssueInvoiceDTO(null, List.of(
                new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(73.20)),
                new PaymentDTO(PaymentMethod.CARD, BigDecimal.valueOf(50.00))
        ), null, null, null);

        billingService.issueInvoice(ACCOUNT_ID, request);

        org.mockito.Mockito.verify(cashShiftService).registerMovement(
                org.mockito.ArgumentMatchers.eq(CASHIER_ID),
                org.mockito.ArgumentMatchers.eq(com.cunoc.restaurant.cashbox.model.MovementType.CASH_SALE),
                org.mockito.ArgumentMatchers.argThat(amount -> amount.compareTo(BigDecimal.valueOf(73.20)) == 0),
                any());
        org.mockito.Mockito.verify(cashShiftService).registerMovement(
                org.mockito.ArgumentMatchers.eq(CASHIER_ID),
                org.mockito.ArgumentMatchers.eq(com.cunoc.restaurant.cashbox.model.MovementType.CARD_SALE),
                org.mockito.ArgumentMatchers.argThat(amount -> amount.compareTo(BigDecimal.valueOf(50.00)) == 0),
                any());
        org.mockito.Mockito.verify(invoicePaymentRepository, org.mockito.Mockito.times(2)).save(any(InvoicePayment.class));
    }

    @Test
    void facturarConClienteRedimeYAcreditaPuntos()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(100), 1)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(customerService.availablePoints(7L)).thenReturn(50, 60); // antes de acreditar, despues de acreditar

        // Subtotal 100 + 12% = 112.00, menos 20 puntos x Q0.10 = Q2.00 de descuento.
        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CARD, BigDecimal.valueOf(110.00))), 7L, 20, null);

        var result = billingService.issueInvoice(ACCOUNT_ID, request);

        org.mockito.Mockito.verify(customerService).redeem(7L, 20, null);
        org.mockito.Mockito.verify(customerService).accrue(
                org.mockito.ArgumentMatchers.eq(7L), any(), any());
        assertThat(result.redeemedPoints()).isEqualTo(20);
        assertThat(result.accruedPoints()).isEqualTo(10);
    }

    // --- Redencion de puntos: el descuento tiene que bajar el total ---------

    /**
     * El bug que esta prueba fija: se redimian los puntos, se descontaban del saldo del
     * cliente y el total seguia igual. El cliente perdia los puntos y pagaba lo mismo.
     */
    @Test
    void redimirPuntosBajaElTotalYQuedaEnLaFactura()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(100), 1)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(customerService.availablePoints(7L)).thenReturn(0, 0);

        // 100 + 12% = 112.00; 50 puntos x Q0.10 = Q5.00 => se pagan Q107.00
        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(107.00))), 7L, 50, null);

        var result = billingService.issueInvoice(ACCOUNT_ID, request);

        assertThat(result.discountAmount()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(result.total()).isEqualByComparingTo(BigDecimal.valueOf(107.00));

        org.mockito.Mockito.verify(cashShiftService).registerMovement(
                org.mockito.ArgumentMatchers.eq(CASHIER_ID),
                org.mockito.ArgumentMatchers.eq(com.cunoc.restaurant.cashbox.model.MovementType.LOYALTY_REDEMPTION),
                org.mockito.ArgumentMatchers.argThat(amount -> amount.compareTo(BigDecimal.valueOf(5.00)) == 0),
                any());
    }

    @Test
    void elDescuentoNuncaDejaElTotalNegativo()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.TEN, 1)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(customerService.availablePoints(7L)).thenReturn(0, 0);

        // 10 + 12% = 11.20, pero se redimen 5000 puntos = Q500: el descuento se topa al bruto.
        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.ZERO)), 7L, 5000, null);

        var result = billingService.issueInvoice(ACCOUNT_ID, request);

        assertThat(result.discountAmount()).isEqualByComparingTo(BigDecimal.valueOf(11.20));
        assertThat(result.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- Propina -------------------------------------------------------------

    /**
     * El otro bug: la propina se calculaba para la precuenta y se guardaba en cero, asi
     * que nunca entraba al total ni generaba CASH_TIP, que es lo que el cuadre suma.
     */
    @Test
    void laPropinaEntraEnElTotalYSeRegistraComoMovimientoAparte()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(100), 1)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        // 100 + 12% = 112.00 + Q15 de propina = Q127.00
        var request = new IssueInvoiceDTO(null,
                List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(127.00))),
                null, null, BigDecimal.valueOf(15.00));

        var result = billingService.issueInvoice(ACCOUNT_ID, request);

        assertThat(result.tipAmount()).isEqualByComparingTo(BigDecimal.valueOf(15.00));
        assertThat(result.total()).isEqualByComparingTo(BigDecimal.valueOf(127.00));

        // La venta y la propina van separadas y suman exactamente lo cobrado.
        org.mockito.Mockito.verify(cashShiftService).registerMovement(
                org.mockito.ArgumentMatchers.eq(CASHIER_ID),
                org.mockito.ArgumentMatchers.eq(com.cunoc.restaurant.cashbox.model.MovementType.CASH_SALE),
                org.mockito.ArgumentMatchers.argThat(amount -> amount.compareTo(BigDecimal.valueOf(112.00)) == 0),
                any());
        org.mockito.Mockito.verify(cashShiftService).registerMovement(
                org.mockito.ArgumentMatchers.eq(CASHIER_ID),
                org.mockito.ArgumentMatchers.eq(com.cunoc.restaurant.cashbox.model.MovementType.CASH_TIP),
                org.mockito.ArgumentMatchers.argThat(amount -> amount.compareTo(BigDecimal.valueOf(15.00)) == 0),
                any());
    }

    @Test
    void conPagoCombinadoLaPropinaSeRepartePeroSumaExacto()
    {
        when(cashShiftService.requireOpenShift(CASHIER_ID)).thenReturn(shift);
        when(tableAccountService.findById(ACCOUNT_ID)).thenReturn(accountWith(deliveredItem(BigDecimal.valueOf(100), 1)));
        when(invoiceRepository.findByTableAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        // Total 122.00 (100 + 12 de impuesto + 10 de propina) en dos pagos de 61.00
        var request = new IssueInvoiceDTO(null, List.of(
                new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(61.00)),
                new PaymentDTO(PaymentMethod.CARD, BigDecimal.valueOf(61.00))
        ), null, null, BigDecimal.valueOf(10.00));

        billingService.issueInvoice(ACCOUNT_ID, request);

        var captor = org.mockito.ArgumentCaptor.forClass(BigDecimal.class);
        org.mockito.Mockito.verify(cashShiftService, org.mockito.Mockito.times(4))
                .registerMovement(any(), any(), captor.capture(), any());

        var suma = captor.getAllValues().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(BigDecimal.valueOf(122.00));
    }
}