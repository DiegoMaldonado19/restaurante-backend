package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long>
{
    List<InvoicePayment> findByInvoiceId(Long invoiceId);
}
