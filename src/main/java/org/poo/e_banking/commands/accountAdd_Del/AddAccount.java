package org.poo.e_banking.commands.accountAdd_Del;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.entities.Account;
import org.poo.entities.BusinessAccount;
import org.poo.entities.Commerciant;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;
import org.poo.fileio.CommerciantInput;

import java.util.List;

public final class AddAccount extends AccountBase {

    public AddAccount(final CommandInput commandInput) {
        super(commandInput);
    }

    private ObjectNode toJson() {
        ObjectNode accountNode = createBaseNode();
        accountNode.put("description", "New account created");
        return accountNode;
    }

    @Override
    public void execute() {
        User user = getUserFromMap();
        if (user == null) {
            return;
        }

        AppLogic appLogic = AppLogic.getInstance();
        if (commandInput.getAccountType().equals("business")) {
            user.addAccount(new BusinessAccount(user.getEmail(), commandInput.getCurrency()));
        } else {
            user.addAccount(commandInput.getAccountType(),
                    commandInput.getCurrency(),
                    commandInput.getInterestRate());
        }

        List<CommerciantInput> commerciants = appLogic.getCommerciants();
        Account account = user.getAccounts().getLast();

        for (CommerciantInput commerciant : commerciants) {
            account.getComerciants().add(new Commerciant(commerciant));
        }

        ObjectNode transactionNode = toJson();
        logTransaction(user, account, transactionNode);
    }
}
