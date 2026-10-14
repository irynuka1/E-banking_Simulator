package org.poo.e_banking.commands.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.entities.Account;
import org.poo.entities.BusinessAccount;
import org.poo.entities.User;

final class BusinessSupport {
    private BusinessSupport() {
    }

    /**
     * Finds an account by IBAN among the accounts owned by all users.
     * @param iban The IBAN to look for.
     * @return The account, or null if there is none.
     */
    static Account findAccount(final String iban) {
        for (User user : AppLogic.getInstance().getUsers()) {
            Account account = user.getAccountByIban(iban);
            if (account != null) {
                return account;
            }
        }
        return null;
    }

    /**
     * Finds a business account by IBAN.
     * @param iban The IBAN to look for.
     * @return The business account, or null if the account is missing or not a business one.
     */
    static BusinessAccount findBusinessAccount(final String iban) {
        Account account = findAccount(iban);
        return account instanceof BusinessAccount business ? business : null;
    }

    /**
     * Adds an error entry in the command output.
     * @param output The output array.
     * @param command The name of the command.
     * @param description The error description.
     * @param timestamp The timestamp of the command.
     */
    static void error(final ArrayNode output, final String command, final String description,
                      final int timestamp) {
        ObjectNode wrapper = new ObjectMapper().createObjectNode();
        wrapper.put("command", command);
        ObjectNode outputNode = wrapper.putObject("output");
        outputNode.put("timestamp", timestamp);
        outputNode.put("description", description);
        wrapper.put("timestamp", timestamp);
        output.add(wrapper);
    }
}
