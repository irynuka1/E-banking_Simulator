package org.poo.e_banking.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.ExchangeRateManager;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.Account;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

public final class WithdrawSavings implements Executable {
    private final CommandInput commandInput;

    public WithdrawSavings(final CommandInput commandInput) {
        this.commandInput = commandInput;
    }

    @Override
    public void execute() {
        final int minAge = 21;
        User user = null;
        Account account = null;
        AppLogic appLogic = AppLogic.getInstance();
        ExchangeRateManager exchangeManager = appLogic.getExchangeRateManager();
        ArrayList<User> users = appLogic.getUsers();

        for (User u : users) {
            if (u.getAccountByIban(commandInput.getAccount()) != null) {
                user = u;
                account = u.getAccountByIban(commandInput.getAccount());
                break;
            }
        }

        if (user == null || account == null) {
            ObjectNode output = new ObjectNode(new ObjectMapper().getNodeFactory());
            output.put("description", "Account not found");
            output.put("timestamp", commandInput.getTimestamp());
            return;
        }

        if (!account.getType().equals("savings")) {
            wrongAccountTypeOutput(user);
            return;
        }

        if (user.getUserAge(user.getBirthdate()) < minAge) {
            notOldEnoughOutput(user);
            return;
        }

        double exchangeRate = exchangeManager.getExchangeRate(account.getCurrency(),
                commandInput.getCurrency());
        double amountToWithdraw = commandInput.getAmount() * exchangeRate;

        Account classic = null;
        for (Account candidate : user.getAccounts()) {
            if (candidate.getType().equals("classic")
                    && candidate.getCurrency().equals(commandInput.getCurrency())) {
                classic = candidate;
                break;
            }
        }

        if (classic == null) {
            noClassicAccountOutput(user, account);
            return;
        }

        if (account.getBalance() < amountToWithdraw) {
            insufficientFundsOutput(user, account);
            return;
        }

        classic.addFunds(commandInput.getAmount());
        account.setBalance(account.getBalance() - amountToWithdraw);
        successOutput(user, account, classic);
    }

    /**
     * Logs the successful withdrawal, once for the savings and once for the classic account
     * @param user the user that made the withdrawal
     * @param savings the savings account
     * @param classic the classic account that received the money
     */
    public void successOutput(final User user, final Account savings, final Account classic) {
        for (Account target : new Account[] {savings, classic}) {
            ObjectNode output = new ObjectNode(new ObjectMapper().getNodeFactory());
            output.put("timestamp", commandInput.getTimestamp());
            output.put("description", "Savings withdrawal");
            output.put("savingsAccountIBAN", savings.getIban());
            output.put("classicAccountIBAN", classic.getIban());
            output.put("amount", commandInput.getAmount());
            user.getTransactionsNode().add(output);
            target.getTransactionsNode().add(output);
        }
    }

    /**
     * Creates a transaction output for the case when the user does not have a classic account
     * @param user the user that made the withdrawal
     * @param account the savings account
     */
    public void noClassicAccountOutput(final User user, final Account account) {
        ObjectNode output = new ObjectNode(new ObjectMapper().getNodeFactory());
        output.put("timestamp", commandInput.getTimestamp());
        output.put("description", "You do not have a classic account.");
        user.getTransactionsNode().add(output);
        account.getTransactionsNode().add(output);
    }

    /**
     * Creates a transaction output for the case when the user does not have enough funds
     * @param user the user that made the withdrawal
     * @param account the savings account
     */
    public void insufficientFundsOutput(final User user, final Account account) {
        ObjectNode output = new ObjectNode(new ObjectMapper().getNodeFactory());
        output.put("description", "Insufficient funds");
        output.put("timestamp", commandInput.getTimestamp());
        user.getTransactionsNode().add(output);
        account.getTransactionsNode().add(output);
    }

    /**
     * Creates a transaction output for the case when the user is under 21
     * @param user the user that made the withdrawal
     */
    public void notOldEnoughOutput(final User user) {
        ObjectNode output = new ObjectNode(new ObjectMapper().getNodeFactory());
        output.put("description", "You don't have the minimum age required.");
        output.put("timestamp", commandInput.getTimestamp());
        user.getTransactionsNode().add(output);
    }

    /**
     * Creates a transaction output for the case when the account is not of type savings
     * @param user the user that made the withdrawal
     */
    public void wrongAccountTypeOutput(final User user) {
        ObjectNode output = new ObjectNode(new ObjectMapper().getNodeFactory());
        output.put("description", "Account is not of type savings.");
        output.put("timestamp", commandInput.getTimestamp());
        user.getTransactionsNode().add(output);
    }
}
