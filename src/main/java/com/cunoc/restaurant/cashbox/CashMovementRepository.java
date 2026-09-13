package com.cunoc.restaurant.cashbox;

import com.cunoc.restaurant.cashbox.model.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long>
{
    List<CashMovement> findByCashShiftId(Long cashShiftId);
}
