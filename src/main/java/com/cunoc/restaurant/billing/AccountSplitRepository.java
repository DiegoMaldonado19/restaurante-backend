package com.cunoc.restaurant.billing;

import com.cunoc.restaurant.billing.model.AccountSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountSplitRepository extends JpaRepository<AccountSplit, Long>
{
    List<AccountSplit> findByTableAccountId(Long tableAccountId);
}
