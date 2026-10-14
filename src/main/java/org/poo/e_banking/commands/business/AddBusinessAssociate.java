package org.poo.e_banking.commands.business;

import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.BusinessAccount;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

public final class AddBusinessAssociate implements Executable {
    private final CommandInput commandInput;

    public AddBusinessAssociate(final CommandInput commandInput) {
        this.commandInput = commandInput;
    }

    @Override
    public void execute() {
        BusinessAccount account = BusinessSupport.findBusinessAccount(commandInput.getAccount());
        User user = AppLogic.getInstance().getUserMap().get(commandInput.getEmail());
        if (account == null || user == null) {
            return;
        }

        if (account.getRole(user.getEmail()) != null) {
            return;
        }

        String role = commandInput.getRole();
        if (!BusinessAccount.MANAGER.equals(role) && !BusinessAccount.EMPLOYEE.equals(role)) {
            return;
        }

        account.getAssociates().put(user.getEmail(), role);
        user.addAssociatedAccount(account);
    }
}
