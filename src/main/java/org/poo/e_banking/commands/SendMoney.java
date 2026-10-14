package org.poo.e_banking.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.BusinessRules;
import org.poo.e_banking.helpers.Commission;
import org.poo.e_banking.helpers.ExchangeRateManager;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.Account;
import org.poo.entities.Commerciant;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;
import org.poo.fileio.CommerciantInput;

import java.util.ArrayList;

public final class SendMoney implements Executable {
    private final CommandInput commandInput;
    private final ArrayNode output;

    public SendMoney(final CommandInput commandInput, final ArrayNode output) {
        this.commandInput = commandInput;
        this.output = output;
    }

    @Override
    public void execute() {
        AppLogic appLogic = AppLogic.getInstance();
        ArrayList<User> users = appLogic.getUsers();
        ExchangeRateManager exchangeManager = appLogic.getExchangeRateManager();

        Account senderAccount = null;
        Account receiverAccount = null;
        User sender = null;
        User receiver = null;

        for (User user : users) {
            if (senderAccount == null) {
                Account found = findAccount(user, commandInput.getAccount());
                if (found != null) {
                    senderAccount = found;
                    sender = user;
                }
            }

            if (receiverAccount == null) {
                Account found = findAccount(user, commandInput.getReceiver());
                if (found != null) {
                    receiverAccount = found;
                    receiver = user;
                }
            }
        }

        if (sender != null && receiverAccount == null
                && findCommerciant() != null) {
            payCommerciant(senderAccount, sender, exchangeManager);
            return;
        }

        processTransaction(senderAccount, receiverAccount, sender, receiver, exchangeManager);
    }

    private CommerciantInput findCommerciant() {
        for (CommerciantInput commerciant : AppLogic.getInstance().getCommerciants()) {
            if (commerciant.getAccount().equals(commandInput.getReceiver())) {
                return commerciant;
            }
        }
        return null;
    }

    private void payCommerciant(final Account senderAccount, final User sender,
                                final ExchangeRateManager exchangeManager) {
        if (!BusinessRules.canSpend(senderAccount, commandInput.getEmail(),
                commandInput.getAmount())) {
            return;
        }

        double amountInRON = commandInput.getAmount()
                * exchangeManager.getExchangeRate(senderAccount.getCurrency(), "RON");
        double commission = Commission.getCommission(
                BusinessRules.planHolder(senderAccount, sender), amountInRON);

        if (senderAccount.withdrawFunds(commandInput.getAmount()
                + commandInput.getAmount() * commission)) {
            BusinessRules.recordSpending(senderAccount, commandInput.getEmail(),
                    commandInput.getAmount(), commandInput.getTimestamp(), null);
            applyCashback(senderAccount, BusinessRules.planHolder(senderAccount, sender),
                    amountInRON);

            ObjectNode node = new ObjectNode(new ObjectMapper().getNodeFactory());
            node.put("timestamp", commandInput.getTimestamp());
            node.put("description", commandInput.getDescription());
            node.put("senderIBAN", senderAccount.getIban());
            node.put("receiverIBAN", commandInput.getReceiver());
            node.put("amount", commandInput.getAmount() + " " + senderAccount.getCurrency());
            node.put("transferType", "sent");
            logTransaction(sender, senderAccount, node);
        } else {
            logTransaction(sender, senderAccount, failedOutput());
        }
    }

    private void applyCashback(final Account account, final User planUser,
                               final double amountInRON) {
        Commerciant commerciant = account.getCommerciant(findCommerciant().getCommerciant());
        if (commerciant == null) {
            return;
        }

        if (commerciant.getCashbackStrategy().equals("nrOfTransactions")) {
            commerciant.getCashBack(account, commandInput.getAmount());
        } else {
            commerciant.getCashBack(amountInRON, account, planUser.getPlan(),
                    commandInput.getAmount());
        }
    }

    private Account findAccount(final User user, final String ibanOrAlias) {
        if (ibanOrAlias == null || ibanOrAlias.isEmpty()) {
            return null;
        }
        Account account = user.getAccountByIban(ibanOrAlias);
        return account != null ? account : user.getAccountByAlias(ibanOrAlias);
    }

    /**
     * Processes the transaction between two accounts.
     * @param senderAccount The account that sends the money.
     * @param receiverAccount The account that receives the money.
     * @param sender The user that sends the money.
     * @param receiver The user that receives the money.
     */
    public void processTransaction(final Account senderAccount, final Account receiverAccount,
                                   final User sender, final User receiver,
                                   final ExchangeRateManager exchangeManager) {
        if (sender == null || receiver == null) {
            ObjectNode wrapper = new ObjectNode(new ObjectMapper().getNodeFactory());
            wrapper.put("command", commandInput.getCommand());
            ObjectNode outputNode = wrapper.putObject("output");
            outputNode.put("description", "User not found");
            outputNode.put("timestamp", commandInput.getTimestamp());
            wrapper.put("timestamp", commandInput.getTimestamp());
            output.add(wrapper);
            return;
        }

        if (senderAccount == null || receiverAccount == null) {
            return;
        }

        double exchangeToRON = exchangeManager.getExchangeRate(senderAccount.getCurrency(), "RON");
        double amountInRON = commandInput.getAmount() * exchangeToRON;

        if (!BusinessRules.canSpend(senderAccount, commandInput.getEmail(),
                commandInput.getAmount())) {
            return;
        }

        double commission = Commission.getCommission(
                BusinessRules.planHolder(senderAccount, sender), amountInRON);

        if (senderAccount.withdrawFunds(commandInput.getAmount()
                + commandInput.getAmount() * commission)) {
            double exchangeRate = exchangeManager.getExchangeRate(senderAccount.getCurrency(),
                    receiverAccount.getCurrency());
            double amountInReceiverCurrency = commandInput.getAmount() * exchangeRate;
            receiverAccount.addFunds(amountInReceiverCurrency);

            BusinessRules.recordSpending(senderAccount, commandInput.getEmail(),
                    commandInput.getAmount(), commandInput.getTimestamp(), null);

            logTransaction(sender, senderAccount, successOutput("sent", senderAccount,
                    receiverAccount,
                    commandInput.getAmount() + " " + senderAccount.getCurrency()));
            logTransaction(receiver, receiverAccount, successOutput("received", senderAccount,
                    receiverAccount,
                    amountInReceiverCurrency + " " + receiverAccount.getCurrency()));
        } else {
            logTransaction(sender, senderAccount, failedOutput());
        }
    }

    /**
     * Creates a node with the details of a successful transaction.
     * @param type The type of the transaction.
     * @param senderAccount The account that sends the money.
     * @param receiverAccount The account that receives the money.
     * @param amount The amount of money.
     * @return The created node.
     */
    public ObjectNode successOutput(final String type, final Account senderAccount,
                                    final Account receiverAccount, final String amount) {
        ObjectNode sendMoneyWrapper = new ObjectNode(new ObjectMapper().getNodeFactory());
        sendMoneyWrapper.put("timestamp", commandInput.getTimestamp());
        sendMoneyWrapper.put("description", commandInput.getDescription());
        sendMoneyWrapper.put("senderIBAN", senderAccount.getIban());
        sendMoneyWrapper.put("receiverIBAN", receiverAccount.getIban());
        sendMoneyWrapper.put("amount", amount);
        sendMoneyWrapper.put("transferType", type);
        return sendMoneyWrapper;
    }

    /**
     * Creates a node with the details in case of insufficient funds.
     * @return The created node.
     */
    public ObjectNode failedOutput() {
        ObjectNode sendMoneyWrapper = new ObjectNode(new ObjectMapper().getNodeFactory());
        sendMoneyWrapper.put("timestamp", commandInput.getTimestamp());
        sendMoneyWrapper.put("description", "Insufficient funds");
        return sendMoneyWrapper;
    }

    /**
     * Adds the transaction in the user's and account's transaction history.
     * @param user The user that initiated the transaction.
     * @param account The account that initiated the transaction.
     * @param transaction The transaction to be logged.
     */
    public void logTransaction(final User user, final Account account,
                               final ObjectNode transaction) {
        user.getTransactionsNode().add(transaction);
        account.getTransactionsNode().add(transaction);
    }
}
