package com.cunoc.restaurant.customer;

import com.cunoc.restaurant.customer.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long>
{
    boolean existsByPhone(String phone);

    boolean existsByPhoneAndCustomerIdNot(String phone, Long customerId);

    // phone es exacto porque es la busqueda del cajero en el mostrador; search es texto libre.
    @Query("""
           SELECT c FROM Customer c
            WHERE (:phone  IS NULL OR c.phone = :phone)
              AND (:search IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR c.phone LIKE CONCAT('%', :search, '%'))
           """)
    Page<Customer> search(@Param("phone")  String phone,
                          @Param("search") String search,
                          Pageable pageable);
}
