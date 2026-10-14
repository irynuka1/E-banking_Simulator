package org.poo.e_banking.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.ExchangeRateManager;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.Account;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.Map;

public final class UpgradePlan implements Executable {
    private final CommandInput commandInput;
    private final ArrayNode output;
    private static final int NECESSARY_TRANSACTIONS = 5;

    public UpgradePlan(final CommandInput commandInput, final ArrayNode output) {
        this.commandInput = commandInput;
        this.output = output;
    }

    @Override
    public void execute() {
        AppLogic appLogic = AppLogic.getInstance();
        Map<String, Integer> planTaxMap = appLogic.getPlanTaxMap();
        ArrayList<User> users = appLogic.getUsers();
        ExchangeRateManager exchangeManager = appLogic.getExchangeRateManager();

        User user = null;
        Account account = null;

        for (User currentUser : users) {
            if (currentUser.getAccountByIban(commandInput.getAccount()) != null) {
                user = currentUser;
                account = user.getAccountByIban(commandInput.getAccount());
                break;
            }
        }

        if (user == null) {
            accountNotFoundOutput();
            return;
        }

        if (freeUpgradeIfPossible(user, account)) {
            return;
        }

        if (user.getPlan().equals(commandInput.getNewPlanType())) {
            ObjectNode node = new ObjectNode(new ObjectMapper().getNodeFactory());
            node.put("timestamp", commandInput.getTimestamp());
            node.put("description", "The user already has the " + user.getPlan() + " plan.");
            logTransaction(user, account, node);
            return;
        }

        String key = user.getPlan() + " -> " + commandInput.getNewPlanType();
        if (planTaxMap.containsKey(key)) {
            double exchangeRate = exchangeManager.getExchangeRate("RON",
                    account.getCurrency());
            double amountInAccountCurrency = planTaxMap.get(key) * exchangeRate;

            if (account.withdrawFunds(amountInAccountCurrency)) {
                user.setPlan(commandInput.getNewPlanType());
                logTransaction(user, account, successOutput());
            } else {
                logTransaction(user, account, insufficientFundsOutput());
            }
        }
    }

    /**
     * Checks if the user has made enough transactions to upgrade the plan and
     * upgrades if so
     * @param user the user
     * @param account the account
     * @return true if the user can upgrade the plan, false otherwise
     */
    public boolean freeUpgradeIfPossible(final User user, final Account account) {
        if (user.getTransactionsToUpgrade() == NECESSARY_TRANSACTIONS) {
            ObjectNode outputNode = new ObjectNode(new ObjectMapper().getNodeFactory());
            outputNode.put("description", "Upgrade plan");
            outputNode.put("accountIBAN", account.getIban());
            outputNode.put("newPlanType", "gold");
            outputNode.put("timestamp", commandInput.getTimestamp());

            user.setPlan("gold");
            logTransaction(user, account, outputNode);
            user.setTransactionsToUpgrade(0);

            return true;
        }
        return false;
    }

    /**
     * Creates the output node for an insufficient funds error
     * @return the output node
     */
    public ObjectNode insufficientFundsOutput() {
        ObjectNode outputNode = new ObjectNode(new ObjectMapper().getNodeFactory());
        outputNode.put("description", "Insufficient funds");
        outputNode.put("timestamp", commandInput.getTimestamp());
        return outputNode;
    }

    /**
     * Creates the output node for an account not found error
     */
    public void accountNotFoundOutput() {
        ObjectNode outputNode = new ObjectNode(new ObjectMapper().getNodeFactory());
        outputNode.put("command", "upgradePlan");
        ObjectNode errorNode = new ObjectNode(new ObjectMapper().getNodeFactory());
        outputNode.set("output", errorNode);
        errorNode.put("description", "Account not found");
        errorNode.put("timestamp", commandInput.getTimestamp());
        outputNode.put("timestamp", commandInput.getTimestamp());
        output.add(outputNode);
    }

    /**
     * Creates the output node for a successful transaction
     * @return the output node
     */
    public ObjectNode successOutput() {
        ObjectNode outputNode = new ObjectNode(new ObjectMapper().getNodeFactory());
        outputNode.put("description", "Upgrade plan");
        outputNode.put("accountIBAN", commandInput.getAccount());
        outputNode.put("newPlanType", commandInput.getNewPlanType());
        outputNode.put("timestamp", commandInput.getTimestamp());
        return outputNode;
    }

    /**
     * Logs a transaction in the user and account transaction nodes
     * @param user the user
     * @param account the account
     * @param outputNode the output node
     */
    public void logTransaction(final User user, final Account account,
                               final ObjectNode outputNode) {
        user.getTransactionsNode().add(outputNode);
        account.getTransactionsNode().add(outputNode);
    }
}
