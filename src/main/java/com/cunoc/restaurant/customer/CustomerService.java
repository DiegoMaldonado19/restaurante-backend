package com.cunoc.restaurant.customer;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.config.RestaurantProperties;
import com.cunoc.restaurant.customer.dto.CreateCustomerDTO;
import com.cunoc.restaurant.customer.dto.CustomerDetailView;
import com.cunoc.restaurant.customer.dto.CustomerView;
import com.cunoc.restaurant.customer.dto.LoyaltyTransactionView;
import com.cunoc.restaurant.customer.dto.UpdateCustomerDTO;
import com.cunoc.restaurant.customer.model.Customer;
import com.cunoc.restaurant.customer.model.LoyaltyTransaction;
import com.cunoc.restaurant.customer.model.LoyaltyTransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Clientes frecuentes y su saldo de puntos. No hay endpoints que muevan puntos:
 * acreditar y redimir ocurren dentro de POST /invoices, en la misma transaccion que
 * la venta, porque unos puntos que se pudieran mover sin una factura detras serian
 * un agujero contable.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService
{
    private final CustomerRepository           customerRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final RestaurantProperties         properties;

    // --- Lo que consume billing ---------------------------------------------

    @Transactional(readOnly = true)
    public int availablePoints(Long customerId)
    {
        findOrFail(customerId);

        return loyaltyTransactionRepository.sumPoints(customerId);
    }

    /** Redime puntos como descuento. Se guarda en negativo para que el saldo siga siendo la suma. */
    public void redeem(Long customerId, int points, Long invoiceId)
    {
        var customer  = findOrFail(customerId);
        var available = loyaltyTransactionRepository.sumPoints(customerId);

        if (points <= 0)
        {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                        "Los puntos a redimir tienen que ser mayores que cero.");
        }

        if (available < points)
        {
            throw new BusinessException(ErrorCode.INSUFFICIENT_LOYALTY_POINTS,
                    "El cliente tiene " + available + " puntos y se intentaron redimir " + points + ".");
        }

        record(customer, LoyaltyTransactionType.REDEMPTION, -points, invoiceId);
    }

    /**
     * Acredita los puntos que genero la compra. Se redondea hacia abajo: no se otorgan
     * fracciones de punto, y una venta que no llega a un punto no ensucia el libro mayor.
     */
    public void accrue(Long customerId, BigDecimal netAmount, Long invoiceId)
    {
        var customer = findOrFail(customerId);
        var points   = netAmount.multiply(pointsPerCurrencyUnit())
                                .setScale(0, RoundingMode.DOWN)
                                .intValue();

        if (points > 0)
        {
            record(customer, LoyaltyTransactionType.ACCRUAL, points, invoiceId);
        }
    }

    // --- Los cinco endpoints ------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CustomerView> search(String phone, String search, Pageable pageable)
    {
        return customerRepository.search(phone, search, pageable).map(CustomerView::from);
    }

    @Transactional(readOnly = true)
    public CustomerDetailView findById(Long customerId)
    {
        return CustomerDetailView.from(
                findOrFail(customerId),
                loyaltyTransactionRepository.sumPoints(customerId),
                loyaltyTransactionRepository.countByCustomerCustomerIdAndTransactionType(
                        customerId, LoyaltyTransactionType.ACCRUAL));
    }

    public CustomerView create(CreateCustomerDTO request)
    {
        if (customerRepository.existsByPhone(request.phone()))
        {
            throw new BusinessException(ErrorCode.CUSTOMER_PHONE_TAKEN,
                    "El telefono " + request.phone() + " ya pertenece a otro cliente.");
        }

        var customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setPhone(request.phone());
        customer.setCreatedAt(LocalDateTime.now());

        return CustomerView.from(customerRepository.save(customer));
    }

    public CustomerView update(Long customerId, UpdateCustomerDTO request)
    {
        var customer = findOrFail(customerId);

        if (customerRepository.existsByPhoneAndCustomerIdNot(request.phone(), customerId))
        {
            throw new BusinessException(ErrorCode.CUSTOMER_PHONE_TAKEN,
                    "El telefono " + request.phone() + " ya pertenece a otro cliente.");
        }

        customer.setFullName(request.fullName());
        customer.setPhone(request.phone());

        return CustomerView.from(customer);
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionView> findLoyaltyTransactions(Long customerId, Pageable pageable)
    {
        findOrFail(customerId);

        return loyaltyTransactionRepository
                .findByCustomerCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(LoyaltyTransactionView::from);
    }

    // --- Interno ------------------------------------------------------------

    /**
     * ponytail: la tasa sale de las propiedades, que son la semilla de restaurant_setting.
     * Cuando restaurant exponga getSettings(), esta linea pasa a leer la tabla, que es la
     * que manda porque el administrador la edita.
     */
    private BigDecimal pointsPerCurrencyUnit()
    {
        return properties.loyalty().pointsPerCurrencyUnit();
    }

    private void record(Customer customer, LoyaltyTransactionType type, int points, Long invoiceId)
    {
        var transaction = new LoyaltyTransaction();
        transaction.setCustomer(customer);
        transaction.setTransactionType(type);
        transaction.setPoints(points);
        transaction.setInvoiceId(invoiceId);
        transaction.setCreatedAt(LocalDateTime.now());

        loyaltyTransactionRepository.save(transaction);
    }

    private Customer findOrFail(Long customerId)
    {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                                                         "No existe el cliente " + customerId + "."));
    }
}
