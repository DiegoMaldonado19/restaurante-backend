package com.cunoc.restaurant.billing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_split")
@Getter
@NoArgsConstructor
public class AccountSplit
{
    @Id
    private Long accountSplitId;

    private Long tableAccountId;

    private String label;
}
