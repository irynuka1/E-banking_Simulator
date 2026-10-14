package org.poo.e_banking.commands;

import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.Account;
import org.poo.entities.BusinessAccount;
import org.poo.entities.BusinessActivity;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

public final class AddFunds implements Executable {
    private final CommandInput commandInput;

    public AddFunds(final CommandInput commandInput) {
        this.commandInput = commandInput;
    }

    @Override
    public void execute() {
        ArrayList<User> users = AppLogic.getInstance().getUsers();

        for (User user : users) {
            Account account = user.getAccountByIban(commandInput.getAccount());
            if (account == null) {
                continue;
            }

            if (account instanceof BusinessAccount business) {
                depositToBusiness(business);
            } else {
                account.addFunds(commandInput.getAmount());
            }
            break;
        }
    }

    private void depositToBusiness(final BusinessAccount account) {
        String role = account.getRole(commandInput.getEmail());
        if (role == null) {
            return;
        }

        if (role.equals(BusinessAccount.EMPLOYEE)
                && commandInput.getAmount() > account.getDepositLimit()) {
            return;
        }

        account.addFunds(commandInput.getAmount());
        if (!role.equals(BusinessAccount.OWNER)) {
            account.record(new BusinessActivity(commandInput.getTimestamp(),
                    commandInput.getEmail(), commandInput.getAmount(), true, null));
        }
    }
}
