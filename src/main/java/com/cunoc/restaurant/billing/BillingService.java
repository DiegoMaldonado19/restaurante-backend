package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.BillPreviewView;
import com.cunoc.restaurant.billing.dto.IssueInvoiceDTO;
import com.cunoc.restaurant.billing.dto.PaymentDTO;
import com.cunoc.restaurant.billing.dto.InvoiceView;
import com.cunoc.restaurant.billing.dto.RateServiceDTO;
import com.cunoc.restaurant.billing.dto.ServiceRatingView;
import com.cunoc.restaurant.billing.dto.VoidInvoiceDTO;
import com.cunoc.restaurant.billing.model.Invoice;
import com.cunoc.restaurant.billing.model.InvoicePayment;
import com.cunoc.restaurant.billing.model.InvoiceStatus;
import com.cunoc.restaurant.billing.model.PaymentMethod;
import com.cunoc.restaurant.billing.model.ServiceRating;
import com.cunoc.restaurant.cashbox.CashShiftService;
import com.cunoc.restaurant.cashbox.model.MovementType;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.common.security.CurrentUser;
import com.cunoc.restaurant.customer.CustomerService;
import com.cunoc.restaurant.ordering.TableAccountService;
import com.cunoc.restaurant.ordering.dto.OrderItemView;
import com.cunoc.restaurant.ordering.dto.OrderTicketView;
import com.cunoc.restaurant.ordering.dto.TableAccountView;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.restaurant.RestaurantSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingService
{
    private final TableAccountService       tableAccountService;
    private final CashShiftService          cashShiftService;
    private final CustomerService           customerService;
    private final InvoiceRepository         invoiceRepository;
    private final InvoiceSequenceRepository invoiceSequenceRepository;
    private final ServiceRatingRepository   serviceRatingRepository;
    private final InvoicePaymentRepository  invoicePaymentRepository;
    private final RestaurantSettingService  settingService;

    @Transactional(readOnly = true)
    public BillPreviewView billPreview(Long accountId)
    {
        var account = tableAccountService.findById(accountId);
        var setting = settingService.get();

        var subtotal = BigDecimal.ZERO;

        for (OrderTicketView ticket : account.tickets())
        {
            for (OrderItemView item : ticket.items())
            {
                if (item.status() == OrderItemStatus.CANCELLED)
                    continue;

                var lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
                subtotal = subtotal.add(lineTotal);
            }
        }

        var taxAmount = percentOf(subtotal, setting.taxPercent());
        var tipAmount = percentOf(subtotal, setting.tipSuggestedPercent());
        var total     = subtotal.add(taxAmount);

        // ponytail: el desglose por sub-cuenta va vacio. Cobrar por sub-cuenta exige que
        // validateNotAlreadyInvoiced deje de mirar la cuenta entera; queda como pendiente.
        return new BillPreviewView(accountId, subtotal, setting.taxPercent(), taxAmount,
                setting.tipSuggestedPercent(), tipAmount, total, List.of());
    }

    /**
     * Factura una cuenta completa. Requiere caja abierta (regla central del proyecto),
     * que la cuenta no tenga items sin entregar, que no este ya facturada, y que la suma
     * de payments[] cuadre exacto con el total (pagos combinados o divididos por sub-cuenta).
     *
     * El total es subtotal + impuesto + propina - descuento por puntos redimidos. El
     * descuento se topa al bruto para que una redencion grande no deje un total negativo.
     */
    public InvoiceView issueInvoice(Long accountId, IssueInvoiceDTO request)
    {
        Long cashierId = CurrentUser.id();
        var shift = cashShiftService.requireOpenShift(cashierId);

        var account = tableAccountService.findById(accountId);

        validateNoPendingItems(account);
        validateNotAlreadyInvoiced(accountId);

        var preview = billPreview(accountId);

        var tipAmount  = request.tipAmount() != null ? request.tipAmount() : BigDecimal.ZERO;
        var grossTotal = preview.subtotal().add(preview.taxAmount()).add(tipAmount);

        int redeemedPoints  = 0;
        var discountAmount  = BigDecimal.ZERO;

        if (request.customerId() != null && request.redeemPoints() != null && request.redeemPoints() > 0)
        {
            redeemedPoints = request.redeemPoints();
            discountAmount = settingService.get().currencyPerPoint()
                    .multiply(BigDecimal.valueOf(redeemedPoints))
                    .setScale(2, RoundingMode.HALF_UP)
                    .min(grossTotal);

            customerService.redeem(request.customerId(), redeemedPoints, null);
        }

        var total = grossTotal.subtract(discountAmount);

        validatePaymentsMatchTotal(request, total);

        var invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setTableAccountId(accountId);
        invoice.setAccountSplitId(request.accountSplitId());
        invoice.setRestaurantTableId(account.restaurantTableId());
        invoice.setCashShiftId(shift.getCashShiftId());
        invoice.setCashierId(cashierId);
        invoice.setWaiterId(account.waiterId());
        invoice.setCustomerId(request.customerId());
        invoice.setSubtotal(preview.subtotal());
        invoice.setDiscountAmount(discountAmount);
        invoice.setTaxAmount(preview.taxAmount());
        invoice.setTipAmount(tipAmount);
        invoice.setTotal(total);
        invoice.setRedeemedPoints(redeemedPoints);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        tableAccountService.close(accountId);

        registerPayments(request.payments(), invoice, cashierId, tipAmount, total);

        if (discountAmount.signum() > 0)
            cashShiftService.registerMovement(cashierId, MovementType.LOYALTY_REDEMPTION,
                    discountAmount, invoice.getInvoiceId());

        if (request.customerId() != null)
        {
            var netSale       = preview.subtotal().subtract(discountAmount).max(BigDecimal.ZERO);
            var accruedPoints = accruePointsAndReturn(request.customerId(), netSale, invoice.getInvoiceId());
            invoice.setAccruedPoints(accruedPoints);
            invoice = invoiceRepository.save(invoice);
        }

        return InvoiceView.from(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceView findInvoiceById(Long invoiceId)
    {
        return InvoiceView.from(invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVOICE_NOT_FOUND,
                        "No existe la factura " + invoiceId + ".")));
    }

    public InvoiceView voidInvoice(Long invoiceId, VoidInvoiceDTO request)
    {
        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVOICE_NOT_FOUND,
                        "No existe la factura " + invoiceId + "."));

        if (invoice.getStatus() == InvoiceStatus.VOIDED)
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_VOIDED,
                    "La factura " + invoiceId + " ya fue anulada.");

        invoice.setStatus(InvoiceStatus.VOIDED);
        invoice.setVoidReason(request.reason());
        invoice.setVoidedAt(LocalDateTime.now());

        return InvoiceView.from(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceView> search(Long waiterId, Long customerId, LocalDateTime from, LocalDateTime to,
                                    Pageable pageable)
    {
        return invoiceRepository.search(waiterId, customerId, from, to, pageable)
                .map(InvoiceView::from);
    }

    public ServiceRatingView rateService(Long invoiceId, RateServiceDTO request)
    {
        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVOICE_NOT_FOUND,
                        "No existe la factura " + invoiceId + "."));

        if (serviceRatingRepository.existsByInvoiceId(invoiceId))
            throw new BusinessException(ErrorCode.RATING_ALREADY_SUBMITTED,
                    "La factura " + invoiceId + " ya tiene una calificación registrada.");

        var rating = new ServiceRating();
        rating.setInvoiceId(invoiceId);
        rating.setWaiterId(invoice.getWaiterId());
        rating.setScore(request.score());
        rating.setCommentText(request.commentText());
        rating.setCreatedAt(LocalDateTime.now());

        return ServiceRatingView.from(serviceRatingRepository.save(rating));
    }

    /**
     * Cada pago entra al turno partido en su venta y su propina: la propina se reparte a
     * prorrata y la ultima linea absorbe el redondeo, de modo que ventas + propinas suma
     * exactamente lo cobrado y el cuadre no cuenta el mismo quetzal dos veces.
     */
    private void registerPayments(List<PaymentDTO> payments, Invoice invoice, Long cashierId,
                                  BigDecimal tipAmount, BigDecimal total)
    {
        var remainingTip = tipAmount;

        for (int i = 0; i < payments.size(); i++)
        {
            var line   = payments.get(i);
            var isCash = line.method() == PaymentMethod.CASH;

            var payment = new InvoicePayment();
            payment.setInvoiceId(invoice.getInvoiceId());
            payment.setMethod(line.method());
            payment.setAmount(line.amount());
            invoicePaymentRepository.save(payment);

            var tipShare = BigDecimal.ZERO;
            if (tipAmount.signum() > 0)
                tipShare = i == payments.size() - 1
                        ? remainingTip
                        : tipAmount.multiply(line.amount()).divide(total, 2, RoundingMode.HALF_UP);

            remainingTip = remainingTip.subtract(tipShare);

            var saleAmount = line.amount().subtract(tipShare);

            if (saleAmount.signum() > 0)
                cashShiftService.registerMovement(cashierId,
                        isCash ? MovementType.CASH_SALE : MovementType.CARD_SALE,
                        saleAmount, invoice.getInvoiceId());

            if (tipShare.signum() > 0)
                cashShiftService.registerMovement(cashierId,
                        isCash ? MovementType.CASH_TIP : MovementType.CARD_TIP,
                        tipShare, invoice.getInvoiceId());
        }
    }

    private static BigDecimal percentOf(BigDecimal amount, BigDecimal percent)
    {
        return amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void validatePaymentsMatchTotal(IssueInvoiceDTO request, BigDecimal total)
    {
        var sum = request.payments().stream()
                .map(p -> p.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(total) != 0)
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "La suma de los pagos (" + sum + ") no coincide con el total de la factura (" + total + ").");
    }

    private int accruePointsAndReturn(Long customerId, BigDecimal netAmount, Long invoiceId)
    {
        var before = customerService.availablePoints(customerId);
        customerService.accrue(customerId, netAmount, invoiceId);
        var after = customerService.availablePoints(customerId);
        return after - before;
    }

    private Long nextInvoiceNumber()
    {
        var sequence = invoiceSequenceRepository.findByIdForUpdate(1L)
                .orElseThrow(() -> new NotFoundException(ErrorCode.VALIDATION_ERROR,
                        "No existe la secuencia de facturacion. Contactar al administrador."));

        var number = sequence.getNextNumber();
        sequence.setNextNumber(number + 1);
        invoiceSequenceRepository.save(sequence);

        return number;
    }

    private void validateNoPendingItems(TableAccountView account)
    {
        var hayPendientes = account.tickets().stream()
                .flatMap(ticket -> ticket.items().stream())
                .anyMatch(item -> item.status() != OrderItemStatus.DELIVERED
                                && item.status() != OrderItemStatus.CANCELLED);

        if (hayPendientes)
            throw new BusinessException(ErrorCode.ACCOUNT_HAS_PENDING_ORDERS,
                    "La cuenta " + account.tableAccountId() + " tiene items sin entregar. Entregarlos antes de facturar.");
    }

    private void validateNotAlreadyInvoiced(Long accountId)
    {
        invoiceRepository.findByTableAccountId(accountId).ifPresent(existing ->
        {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_INVOICED,
                    "La cuenta " + accountId + " ya fue facturada (factura " + existing.getInvoiceNumber() + ").");
        });
    }
}