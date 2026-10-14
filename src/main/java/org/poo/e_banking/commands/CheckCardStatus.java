package org.poo.e_banking.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.Account;
import org.poo.entities.Card;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

public final class CheckCardStatus implements Executable {
    private final CommandInput commandInput;
    private final ArrayNode output;

    public CheckCardStatus(final CommandInput commandInput, final ArrayNode output) {
        this.commandInput = commandInput;
        this.output = output;
    }

    @Override
    public void execute() {
        ArrayList<User> users = AppLogic.getInstance().getUsers();

        User user = getUserByCardNumber(users, commandInput.getCardNumber());
        if (user == null) {
            errorOutput();
            return;
        }

        Card card = user.getCardByNumber(commandInput.getCardNumber());
        Account account = user.getAccessibleAccountByIban(card.getAssociatedIban());
        String previousStatus = card.getStatus();
        card.updateStatus(account.getBalance(), account.getMinBalance());

        if (!previousStatus.equals(card.getStatus())) {
            ObjectNode statusNode = buildStatusOutputNode(card.getStatus());
            user.getTransactionsNode().add(statusNode);
        }
    }

    /**
     * Returns the user that has the card with the given card number.
     *
     * @param cardNumber the card number
     * @return the user that has the card with the given card number or null
     */
    public User getUserByCardNumber(final ArrayList<User> users, final String cardNumber) {
        for (User user : users) {
            Card card = user.getCardByNumber(cardNumber);
            if (card != null) {
                return user;
            }
        }

        return null;
    }

    /**
     * Creates the status output node.
     *
     * @param status the status
     * @return the output node
     */
    private ObjectNode buildStatusOutputNode(final String status) {
        ObjectNode node = new ObjectNode(new ObjectMapper().getNodeFactory());
        node.put("timestamp", commandInput.getTimestamp());
        if ("frozen".equals(status)) {
            node.put("description",
                    "You have reached the minimum amount of funds, the card will be frozen");
        }

        return node;
    }

    /**
     * Creates the error output node.
     */
    public void errorOutput() {
        ObjectNode node = new ObjectNode(new ObjectMapper().getNodeFactory());
        node.put("command", commandInput.getCommand());

        ObjectNode outputNode = node.putObject("output");
        outputNode.put("description", "Card not found");
        outputNode.put("timestamp", commandInput.getTimestamp());

        node.put("timestamp", commandInput.getTimestamp());
        output.add(node);
    }
}
