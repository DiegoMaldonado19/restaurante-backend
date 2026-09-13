package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.ordering.model.AccountSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountSplitRepository extends JpaRepository<AccountSplit, Long>
{
    List<AccountSplit> findByAccountTableAccountId(@Param("accountId") Long accountId);
}
