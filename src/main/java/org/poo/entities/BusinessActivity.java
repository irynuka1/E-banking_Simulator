package org.poo.entities;

import lombok.Getter;

@Getter
public final class BusinessActivity {
    private final int timestamp;
    private final String email;
    private final double amount;
    private final boolean deposit;
    private final String commerciant;

    public BusinessActivity(final int timestamp, final String email, final double amount,
                            final boolean deposit, final String commerciant) {
        this.timestamp = timestamp;
        this.email = email;
        this.amount = amount;
        this.deposit = deposit;
        this.commerciant = commerciant;
    }
}
