package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.ServiceRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRatingRepository extends JpaRepository<ServiceRating, Long>
{
    Optional<ServiceRating> findByInvoiceId(Long invoiceId);

    boolean existsByInvoiceId(Long invoiceId);
}
