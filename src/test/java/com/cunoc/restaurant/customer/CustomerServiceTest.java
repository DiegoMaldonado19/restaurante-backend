package com.cunoc.restaurant.customer;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.config.RestaurantProperties;
import com.cunoc.restaurant.customer.model.Customer;
import com.cunoc.restaurant.customer.model.LoyaltyTransaction;
import com.cunoc.restaurant.customer.model.LoyaltyTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** El libro mayor de puntos: el saldo es la suma con signo y no se puede redimir de mas. */
class CustomerServiceTest
{
    private static final Long CUSTOMER_ID = 1L;
    private static final Long INVOICE_ID  = 99L;

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);

    private final LoyaltyTransactionRepository loyaltyTransactionRepository =
            mock(LoyaltyTransactionRepository.class);

    private final RestaurantProperties properties = new RestaurantProperties(
            null, null, new RestaurantProperties.Loyalty(BigDecimal.ONE));

    private final CustomerService customerService =
            new CustomerService(customerRepository, loyaltyTransactionRepository, properties);

    private final List<LoyaltyTransaction> ledger = new ArrayList<>();

    @BeforeEach
    void setUp()
    {
        var customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setFullName("Ana Lopez");
        customer.setPhone("50212345678");

        ledger.clear();

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class)))
                .thenAnswer(invocation ->
                {
                    var transaction = invocation.<LoyaltyTransaction>getArgument(0);
                    ledger.add(transaction);

                    return transaction;
                });
        when(loyaltyTransactionRepository.sumPoints(CUSTOMER_ID))
                .thenAnswer(invocation -> ledger.stream()
                        .mapToInt(LoyaltyTransaction::getPoints)
                        .sum());
    }

    @Test
    void acreditaYRedimeDejandoElSaldoEnLaSumaConSigno()
    {
        customerService.accrue(CUSTOMER_ID, new BigDecimal("250.00"), INVOICE_ID);
        customerService.redeem(CUSTOMER_ID, 100, INVOICE_ID);

        assertThat(customerService.availablePoints(CUSTOMER_ID)).isEqualTo(150);
        assertThat(ledger).extracting(LoyaltyTransaction::getPoints).containsExactly(250, -100);
    }

    @Test
    void laRedencionSeGuardaEnNegativo()
    {
        customerService.accrue(CUSTOMER_ID, new BigDecimal("100.00"), INVOICE_ID);
        customerService.redeem(CUSTOMER_ID, 40, INVOICE_ID);

        assertThat(ledger).last()
                .satisfies(transaction ->
                {
                    assertThat(transaction.getTransactionType())
                            .isEqualTo(LoyaltyTransactionType.REDEMPTION);
                    assertThat(transaction.getPoints()).isNegative();
                });
    }

    @Test
    void noSePuedeRedimirMasDeLoQueElClienteTiene()
    {
        customerService.accrue(CUSTOMER_ID, new BigDecimal("50.00"), INVOICE_ID);

        assertThatThrownBy(() -> customerService.redeem(CUSTOMER_ID, 51, INVOICE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_LOYALTY_POINTS);
    }

    @Test
    void unaVentaQueNoLlegaAUnPuntoNoEnsuciaElLibroMayor()
    {
        customerService.accrue(CUSTOMER_ID, new BigDecimal("0.80"), INVOICE_ID);

        assertThat(ledger).isEmpty();
    }
}
