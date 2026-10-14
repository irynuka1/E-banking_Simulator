package org.poo.entities;

import lombok.Getter;
import org.poo.e_banking.AppLogic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class BusinessAccount extends Account {
    public static final String OWNER = "owner";
    public static final String MANAGER = "manager";
    public static final String EMPLOYEE = "employee";

    private static final double DEFAULT_LIMIT_RON = 500;

    private Double spendingLimit;
    private Double depositLimit;
    private final Map<String, String> associates = new LinkedHashMap<>();
    private final List<BusinessActivity> activities = new ArrayList<>();

    public BusinessAccount(final String userEmail, final String currency) {
        super(userEmail, currency);
        this.setType("business");
    }

    /**
     * Returns the spending limit for employees, in the currency of the account.
     * Until the owner changes it, it is the default limit converted from RON.
     * @return The spending limit.
     */
    public double getSpendingLimit() {
        return spendingLimit != null ? spendingLimit : defaultLimit();
    }

    /**
     * Returns the deposit limit for employees, in the currency of the account.
     * Until the owner changes it, it is the default limit converted from RON.
     * @return The deposit limit.
     */
    public double getDepositLimit() {
        return depositLimit != null ? depositLimit : defaultLimit();
    }

    private double defaultLimit() {
        return DEFAULT_LIMIT_RON * AppLogic.getInstance().getExchangeRateManager()
                .getExchangeRate("RON", getCurrency());
    }

    public void setSpendingLimit(final double limit) {
        this.spendingLimit = limit;
    }

    public void setDepositLimit(final double limit) {
        this.depositLimit = limit;
    }

    /**
     * Returns the role of a user on this account.
     * @param email The email of the user.
     * @return The role, or null if the user is not related to the account.
     */
    public String getRole(final String email) {
        if (email == null) {
            return null;
        }
        if (email.equals(getUserEmail())) {
            return OWNER;
        }
        return associates.get(email);
    }

    /**
     * Records a deposit or a payment made by a manager or an employee.
     * @param activity The activity to record.
     */
    public void record(final BusinessActivity activity) {
        activities.add(activity);
    }
}
