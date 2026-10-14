package org.poo.e_banking.commands.business;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.BusinessAccount;
import org.poo.fileio.CommandInput;

public final class ChangeLimit implements Executable {
    private final CommandInput commandInput;
    private final ArrayNode output;
    private final boolean spending;

    /**
     * Creates the command.
     * @param commandInput The command input.
     * @param output The output array.
     * @param spending True for changeSpendingLimit, false for changeDepositLimit.
     */
    public ChangeLimit(final CommandInput commandInput, final ArrayNode output,
                       final boolean spending) {
        this.commandInput = commandInput;
        this.output = output;
        this.spending = spending;
    }

    @Override
    public void execute() {
        var account = BusinessSupport.findAccount(commandInput.getAccount());
        if (account == null) {
            return;
        }

        String kind = spending ? "spending" : "deposit";
        if (!(account instanceof BusinessAccount business)) {
            BusinessSupport.error(output, commandInput.getCommand(),
                    "This is not a business account", commandInput.getTimestamp());
            return;
        }

        if (!BusinessAccount.OWNER.equals(business.getRole(commandInput.getEmail()))) {
            BusinessSupport.error(output, commandInput.getCommand(),
                    "You must be owner in order to change " + kind + " limit.",
                    commandInput.getTimestamp());
            return;
        }

        if (spending) {
            business.setSpendingLimit(commandInput.getAmount());
        } else {
            business.setDepositLimit(commandInput.getAmount());
        }
    }
}
