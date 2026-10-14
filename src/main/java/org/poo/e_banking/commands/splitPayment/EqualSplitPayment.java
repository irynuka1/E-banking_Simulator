package org.poo.e_banking.commands.splitPayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.ExchangeRateManager;
import org.poo.entities.Account;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EqualSplitPayment implements SplitPaymentStrategy {
    @Override
    public void execute(final CommandInput commandInput, final AppLogic appLogic) {
        Map<String, User> userMap = appLogic.getUserMap();
        ArrayList<User> users = appLogic.getUsers();
        ExchangeRateManager exchangeManager = appLogic.getExchangeRateManager();

        List<Account> accounts = getParticipatingAccounts(users, commandInput.getAccounts());
        List<Double> amounts = getAmounts(commandInput);

        Account insufficientFundsAcc = checkAccountsBalance(accounts, amounts, exchangeManager,
                commandInput.getCurrency());

        if (insufficientFundsAcc == null) {
            processSuccessfulPayment(accounts, amounts, exchangeManager, userMap, commandInput);
        } else {
            processFailedPayment(accounts, amounts, insufficientFundsAcc, userMap, commandInput);
        }
    }

    /**
     * Creates a list with the amounts that each account has to pay.
     *
     * @param commandInput The command input that contains the information about the split payment.
     * @return The list of amounts equal for each participant.
     */
    public List<Double> getAmounts(final CommandInput commandInput) {
        int numberOfParticipants = commandInput.getAccounts().size();
        double amountPerParticipant = commandInput.getAmount() / numberOfParticipants;
        List<Double> amounts = new ArrayList<>();

        for (int i = 0; i < numberOfParticipants; i++) {
            amounts.add(amountPerParticipant);
        }

        return amounts;
    }

    @Override
    public ObjectNode successOutput(final CommandInput commandInput, final List<Double> amounts) {
        ObjectNode splitPaymentWrapper = new ObjectNode(new ObjectMapper().getNodeFactory());
        splitPaymentWrapper.put("timestamp", commandInput.getTimestamp());
        splitPaymentWrapper.put("description", "Split payment of "
                + String.format("%.2f", commandInput.getAmount()) + " "
                + commandInput.getCurrency());
        splitPaymentWrapper.put("splitPaymentType", commandInput.getSplitPaymentType());
        splitPaymentWrapper.put("currency", commandInput.getCurrency());
        splitPaymentWrapper.put("amount", amounts.getFirst());
        addInvolvedAccounts(splitPaymentWrapper, commandInput);

        return splitPaymentWrapper;
    }

    @Override
    public ObjectNode failedOutput(final CommandInput commandInput, final List<Double> amounts,
                                   final Account insufficientFundsAcc) {
        ObjectNode splitPaymentWrapper = new ObjectNode(new ObjectMapper().getNodeFactory());
        splitPaymentWrapper.put("amount", amounts.getFirst());
        splitPaymentWrapper.put("currency", commandInput.getCurrency());
        splitPaymentWrapper.put("description", "Split payment of "
                + String.format("%.2f", commandInput.getAmount()) + " "
                + commandInput.getCurrency());
        splitPaymentWrapper.put("error", "Account " + insufficientFundsAcc.getIban()
                + " has insufficient funds for a split payment.");
        addInvolvedAccounts(splitPaymentWrapper, commandInput);
        splitPaymentWrapper.put("splitPaymentType", commandInput.getSplitPaymentType());
        splitPaymentWrapper.put("timestamp", commandInput.getTimestamp());

        return splitPaymentWrapper;
    }
}
