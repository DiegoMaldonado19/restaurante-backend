package com.cunoc.restaurant.cashbox;

import com.cunoc.restaurant.cashbox.model.CashShift;
import com.cunoc.restaurant.cashbox.model.CashShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CashShiftRepository extends JpaRepository<CashShift, Long>
{
    Optional<CashShift> findByCashierIdAndStatus(Long cashierId, CashShiftStatus status);

    List<CashShift> findByCashierId(Long cashierId);

    List<CashShift> findByStatus(CashShiftStatus status);
}
