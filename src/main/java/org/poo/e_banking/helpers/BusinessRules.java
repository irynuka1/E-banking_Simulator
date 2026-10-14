package org.poo.e_banking.helpers;

import org.poo.e_banking.AppLogic;
import org.poo.entities.Account;
import org.poo.entities.BusinessAccount;
import org.poo.entities.BusinessActivity;
import org.poo.entities.User;

public final class BusinessRules {
    private BusinessRules() {
    }

    /**
     * Checks if the user is allowed to spend the amount from the account.
     * Employees are limited by the spending limit of a business account.
     * @param account The account the money is taken from.
     * @param email The email of the user that initiates the payment.
     * @param amount The amount, in the currency of the account.
     * @return True if the payment may proceed.
     */
    public static boolean canSpend(final Account account, final String email,
                                   final double amount) {
        if (!(account instanceof BusinessAccount business)) {
            return true;
        }

        String role = business.getRole(email);
        if (role == null) {
            return false;
        }

        return !role.equals(BusinessAccount.EMPLOYEE) || amount <= business.getSpendingLimit();
    }

    /**
     * Stores a payment made by a manager or an employee of a business account.
     * @param account The account the money was taken from.
     * @param email The email of the user that made the payment.
     * @param amount The amount, in the currency of the account.
     * @param timestamp The timestamp of the payment.
     * @param commerciant The commerciant that received the money (may be null).
     */
    public static void recordSpending(final Account account, final String email,
                                      final double amount, final int timestamp,
                                      final String commerciant) {
        if (account instanceof BusinessAccount business) {
            String role = business.getRole(email);
            if (BusinessAccount.MANAGER.equals(role) || BusinessAccount.EMPLOYEE.equals(role)) {
                business.record(new BusinessActivity(timestamp, email, amount, false,
                        commerciant));
            }
        }
    }

    /**
     * Returns the user whose plan applies to operations on the account.
     * For business accounts this is the owner, otherwise the given user.
     * @param account The account used for the operation.
     * @param fallback The user that initiated the operation.
     * @return The user whose plan applies.
     */
    public static User planHolder(final Account account, final User fallback) {
        if (account instanceof BusinessAccount) {
            User owner = AppLogic.getInstance().getUserMap().get(account.getUserEmail());
            if (owner != null) {
                return owner;
            }
        }
        return fallback;
    }
}
