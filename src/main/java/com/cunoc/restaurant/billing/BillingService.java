package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.dto.BillPreviewView;
import com.cunoc.restaurant.billing.dto.IssueInvoiceDTO;
import com.cunoc.restaurant.billing.dto.PaymentDTO;
import com.cunoc.restaurant.billing.dto.InvoiceLineView;
import com.cunoc.restaurant.billing.dto.InvoiceView;
import com.cunoc.restaurant.billing.dto.RateServiceDTO;
import com.cunoc.restaurant.billing.dto.ServiceRatingView;
import com.cunoc.restaurant.billing.dto.SplitPreviewView;
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
import com.cunoc.restaurant.menu.MenuService;
import com.cunoc.restaurant.ordering.TableAccountService;
import com.cunoc.restaurant.ordering.dto.OrderItemView;
import com.cunoc.restaurant.ordering.dto.OrderTicketView;
import com.cunoc.restaurant.ordering.dto.TableAccountView;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.ordering.model.SplitMode;
import com.cunoc.restaurant.restaurant.RestaurantSettingService;
import com.cunoc.restaurant.restaurant.RestaurantTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final MenuService               menuService;
    private final RestaurantTableService    tableService;

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
                if (!esVigente(item))
                    continue;

                var lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
                subtotal = subtotal.add(lineTotal);
            }
        }

        var taxAmount = percentOf(subtotal, setting.taxPercent());
        var tipAmount = percentOf(subtotal, setting.tipSuggestedPercent());
        var total     = subtotal.add(taxAmount);

        return new BillPreviewView(accountId, subtotal, setting.taxPercent(), taxAmount,
                setting.tipSuggestedPercent(), tipAmount, total, splitsOf(account));
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
        validateNotAlreadyInvoiced(accountId, request.accountSplitId());

        var preview = billPreview(accountId);

        var subtotal   = subtotalToCharge(preview, request.accountSplitId());
        var taxAmount  = percentOf(subtotal, preview.taxPercent());
        var tipAmount  = request.tipAmount() != null ? request.tipAmount() : BigDecimal.ZERO;
        var grossTotal = subtotal.add(taxAmount).add(tipAmount);

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
        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTipAmount(tipAmount);
        invoice.setTotal(total);
        invoice.setRedeemedPoints(redeemedPoints);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        // Dividida, la cuenta se cierra cuando se cobra la ultima sub-cuenta; entera, de una vez.
        if (request.accountSplitId() == null || invoiceRepository.findByTableAccountId(accountId).size() >= preview.splits().size())
            tableAccountService.close(accountId);

        registerPayments(request.payments(), invoice, cashierId, tipAmount, total);

        if (discountAmount.signum() > 0)
            cashShiftService.registerMovement(cashierId, MovementType.LOYALTY_REDEMPTION,
                    discountAmount, invoice.getInvoiceId());

        if (request.customerId() != null)
        {
            var netSale       = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
            var accruedPoints = accruePointsAndReturn(request.customerId(), netSale, invoice.getInvoiceId());
            invoice.setAccruedPoints(accruedPoints);
            invoice = invoiceRepository.save(invoice);
        }

        return InvoiceView.from(invoice);
    }

    /**
     * El comprobante: la factura con su detalle de platillos, que es lo que la vista de
     * impresion necesita. El historial (search) no lo trae: resolver el detalle de cada
     * fila de una pagina son N lecturas de cuenta para datos que la lista no enseña.
     */
    @Transactional(readOnly = true)
    public InvoiceView findInvoiceById(Long invoiceId)
    {
        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVOICE_NOT_FOUND,
                        "No existe la factura " + invoiceId + "."));

        return InvoiceView.of(invoice, linesOf(invoice),
                tableService.findById(invoice.getRestaurantTableId()).tableNumber());
    }

    /**
     * Las lineas salen de la cuenta, no de la factura: el precio ya viene congelado en el
     * order_item, asi que el comprobante suma igual que billPreview. Se omiten las
     * canceladas, que tampoco entraron al subtotal.
     *
     * ponytail: el nombre del platillo se resuelve al imprimir, no al facturar, asi que
     * renombrar un platillo renombra los comprobantes viejos. Congelarlo exige una tabla
     * invoice_line y rellenarla para las facturas que ya existen.
     */
    private List<InvoiceLineView> linesOf(Invoice invoice)
    {
        var account    = tableAccountService.findById(invoice.getTableAccountId());
        var splitItems = itemIdsOfSplit(account, invoice.getAccountSplitId());
        var lines      = new ArrayList<InvoiceLineView>();
        Map<Long, String> dishNames = new HashMap<>();

        for (OrderTicketView ticket : account.tickets())
        {
            for (OrderItemView item : ticket.items())
            {
                if (!esVigente(item))
                    continue;

                if (!splitItems.isEmpty() && !splitItems.contains(item.orderItemId()))
                    continue;

                var dishName = dishNames.computeIfAbsent(item.dishId(),
                        id -> menuService.dishDetail(id).name());

                lines.add(InvoiceLineView.of(item, dishName));
            }
        }

        return lines;
    }

    /**
     * Los items que reparte la sub-cuenta, o vacio si la factura los cubre todos. Solo la
     * division por item reparte platillos: la division por persona es una fraccion de la
     * cuenta entera, asi que su comprobante lleva el detalle completo.
     */
    private Set<Long> itemIdsOfSplit(TableAccountView account, Long accountSplitId)
    {
        if (accountSplitId == null || account.splits() == null || account.splits().accounts() == null)
            return Set.of();

        return account.splits().accounts().stream()
                .filter(split -> accountSplitId.equals(split.accountSplitId()))
                .filter(split -> split.mode() == SplitMode.BY_ITEM)
                .findFirst()
                .map(split -> split.items().stream().map(OrderItemView::orderItemId).collect(Collectors.toSet()))
                .orElseGet(Set::of);
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
                .anyMatch(item -> esVigente(item) && item.status() != OrderItemStatus.DELIVERED);

        if (hayPendientes)
            throw new BusinessException(ErrorCode.ACCOUNT_HAS_PENDING_ORDERS,
                    "La cuenta " + account.tableAccountId() + " tiene items sin entregar. Entregarlos antes de facturar.");
    }

    /** Un item UNAVAILABLE ya devolvio su stock: ni se cobra ni bloquea el cobro, igual que uno anulado. */
    private static boolean esVigente(OrderItemView item)
    {
        return item.status() != OrderItemStatus.CANCELLED
            && item.status() != OrderItemStatus.UNAVAILABLE;
    }

    /**
     * Cobrar dividido factura una sub-cuenta a la vez, asi que lo que no se puede repetir
     * es la sub-cuenta, no la cuenta. Sin sub-cuenta se sigue mirando la cuenta entera.
     */
    private void validateNotAlreadyInvoiced(Long accountId, Long accountSplitId)
    {
        if (accountSplitId != null)
        {
            invoiceRepository.findByAccountSplitId(accountSplitId).ifPresent(existing ->
            {
                throw new BusinessException(ErrorCode.SPLIT_ALREADY_INVOICED,
                        "La sub-cuenta " + accountSplitId + " ya fue facturada (factura " + existing.getInvoiceNumber() + ").");
            });
            return;
        }

        invoiceRepository.findByTableAccountId(accountId).stream().findFirst().ifPresent(existing ->
        {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_INVOICED,
                    "La cuenta " + accountId + " ya fue facturada (factura " + existing.getInvoiceNumber() + ").");
        });
    }

    private List<SplitPreviewView> splitsOf(TableAccountView account)
    {
        if (account.splits() == null || account.splits().accounts() == null)
            return List.of();

        return account.splits().accounts().stream()
                .map(split -> new SplitPreviewView(split.accountSplitId(), split.label(), split.shareAmount()))
                .toList();
    }

    /** El subtotal de la sub-cuenta cuando se cobra dividido; el de la cuenta entera si no. */
    private static BigDecimal subtotalToCharge(BillPreviewView preview, Long accountSplitId)
    {
        if (accountSplitId == null)
            return preview.subtotal();

        return preview.splits().stream()
                .filter(split -> accountSplitId.equals(split.accountSplitId()))
                .findFirst()
                .map(SplitPreviewView::subtotal)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "La sub-cuenta " + accountSplitId + " no pertenece a la cuenta " + preview.accountId() + "."));
    }
}