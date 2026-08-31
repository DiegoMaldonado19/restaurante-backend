package com.cunoc.restaurant.customer;

import com.cunoc.restaurant.customer.model.LoyaltyTransaction;
import com.cunoc.restaurant.customer.model.LoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long>
{
    Page<LoyaltyTransaction> findByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId,
                                                                         Pageable pageable);

    /** El saldo es la suma con signo: las redenciones se guardan en negativo. */
    @Query("""
           SELECT COALESCE(SUM(t.points), 0) FROM LoyaltyTransaction t
            WHERE t.customer.customerId = :customerId
           """)
    int sumPoints(@Param("customerId") Long customerId);

    /** Una acreditacion por factura, asi que contarlas es contar visitas. */
    long countByCustomerCustomerIdAndTransactionType(Long customerId, LoyaltyTransactionType type);
}
