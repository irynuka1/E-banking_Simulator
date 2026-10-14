package org.poo.e_banking.commands.cardAdd_Del;

import org.poo.entities.Account;
import org.poo.entities.Card;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

public final class DeleteCard extends CardBase {
    public DeleteCard(final CommandInput commandInput) {
        super(commandInput);
    }

    /**
     * Process the command to delete a card
     * @param user the user that wants to delete a card
     */
    @Override
    protected void processCardCommand(final User user) {
        Card card = user.getCardByNumber(commandInput.getCardNumber());
        if (card != null) {
            Account account = user.getAccessibleAccountByIban(card.getAssociatedIban());
            if (account.getBalance() != 0) {
                return;
            }
            account.removeCard(commandInput.getCardNumber());

            user.getTransactionsNode().add(toJson(user,
                    "The card has been destroyed",
                    commandInput.getCardNumber(),
                    account.getIban()));
        }
    }
}
