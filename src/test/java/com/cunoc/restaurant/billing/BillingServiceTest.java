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

    private final BillingService billingService = new BillingService(
            tableAccountService, cashShiftService, customerService,
            invoiceRepository, invoiceSequenceRepository, serviceRatingRepository, invoicePaymentRepository);

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

        // application.properties trae estos valores; los fijamos aqui via reflection
        // porque @Value no se resuelve sin contexto de Spring en un test unitario puro.
        setField("taxPercent", BigDecimal.valueOf(12));
        setField("suggestedTipPercent", BigDecimal.valueOf(10));

        when(invoiceSequenceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sequence));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePaymentRepository.save(any(InvoicePayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cashShiftService.registerMovement(any(), any(), any(), any()))
                .thenReturn(mock(CashMovementView.class));
    }

    private void setField(String name, Object value)
    {
        try
        {
            var field = BillingService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(billingService, value);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(100))), null, null);

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

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.TEN)), null, null);

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

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(61.60))), null, null);

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

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(100))), null, null);

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

        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CASH, BigDecimal.valueOf(123.20))), null, null);

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
        ), null, null);

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

        // Subtotal 100 + 12% = 112.00
        var request = new IssueInvoiceDTO(null, List.of(new PaymentDTO(PaymentMethod.CARD, BigDecimal.valueOf(112.00))), 7L, 20);

        var result = billingService.issueInvoice(ACCOUNT_ID, request);

        org.mockito.Mockito.verify(customerService).redeem(7L, 20, null);
        org.mockito.Mockito.verify(customerService).accrue(
                org.mockito.ArgumentMatchers.eq(7L), any(), any());
        assertThat(result.redeemedPoints()).isEqualTo(20);
        assertThat(result.accruedPoints()).isEqualTo(10);
    }
}