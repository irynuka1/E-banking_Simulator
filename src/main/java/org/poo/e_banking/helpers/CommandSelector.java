package org.poo.e_banking.helpers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.e_banking.commands.AddFunds;
import org.poo.e_banking.commands.PrintUsers;
import org.poo.e_banking.commands.SendMoney;
import org.poo.e_banking.commands.SetMinBalance;
import org.poo.e_banking.commands.accountAdd_Del.AddAccount;
import org.poo.e_banking.commands.accountAdd_Del.DeleteAccount;
import org.poo.e_banking.commands.business.AddBusinessAssociate;
import org.poo.e_banking.commands.business.BusinessReport;
import org.poo.e_banking.commands.business.ChangeLimit;
import org.poo.e_banking.commands.cardAdd_Del.CreateCard;
import org.poo.e_banking.commands.cardAdd_Del.DeleteCard;
import org.poo.e_banking.commands.interestRate.AddInterestRate;
import org.poo.e_banking.commands.interestRate.ChangeInterestRate;
import org.poo.e_banking.commands.payOnlineCommand.PayOnline;
import org.poo.e_banking.commands.reports.Report;
import org.poo.e_banking.commands.reports.SpendingsReport;
import org.poo.e_banking.commands.splitPayment.VerifyCustomSplit;
import org.poo.e_banking.commands.splitPayment.VerifyEqualSplit;
import org.poo.fileio.CommandInput;
import org.poo.e_banking.commands.AcceptRejectPayment;
import org.poo.e_banking.commands.CheckCardStatus;
import org.poo.e_banking.commands.CashWithdrawal;
import org.poo.e_banking.commands.PrintTransactions;
import org.poo.e_banking.commands.SetAlias;
import org.poo.e_banking.commands.UpgradePlan;
import org.poo.e_banking.commands.WithdrawSavings;

public final class CommandSelector {
    private CommandSelector() {
    }

    /**
     * Selects the command to be executed based on the input command.
     *
     * @param commandInput      The input.
     * @param output            The output array.
     */
    public static void selectCommand(final CommandInput commandInput, final ArrayNode output) {
        Executable command = null;

        switch (commandInput.getCommand()) {
            case "printUsers":
                command = new PrintUsers(commandInput, output);
                break;
            case "addAccount":
                command = new AddAccount(commandInput);
                break;
            case "createCard":
                command = new CreateCard(commandInput, "Normal");
                break;
            case "createOneTimeCard":
                command = new CreateCard(commandInput, "OneTime");
                break;
            case "addFunds":
                command = new AddFunds(commandInput);
                break;
            case "deleteAccount":
                command = new DeleteAccount(commandInput, output);
                break;
            case "deleteCard":
                command = new DeleteCard(commandInput);
                break;
            case "setMinimumBalance":
                command = new SetMinBalance(commandInput);
                break;
            case "payOnline":
                command = new PayOnline(commandInput, output);
                break;
            case "sendMoney":
                command = new SendMoney(commandInput, output);
                break;
            case "printTransactions":
                command = new PrintTransactions(commandInput, output);
                break;
            case "setAlias":
                command = new SetAlias(commandInput);
                break;
            case "checkCardStatus":
                command = new CheckCardStatus(commandInput, output);
                break;
            case "changeInterestRate":
                command = new ChangeInterestRate(commandInput, output);
                break;
            case "splitPayment":
                if (commandInput.getSplitPaymentType().equals("equal")) {
                    command = new VerifyEqualSplit(commandInput, output);
                } else {
                    command = new VerifyCustomSplit(commandInput, output);
                }
                break;
            case "report":
                command = new Report(commandInput, output);
                break;
            case "spendingsReport":
                command = new SpendingsReport(commandInput, output);
                break;
            case "addInterest":
                command = new AddInterestRate(commandInput, output);
                break;
            case "withdrawSavings":
                command = new WithdrawSavings(commandInput);
                break;
            case "upgradePlan":
                command = new UpgradePlan(commandInput, output);
                break;
            case "addNewBusinessAssociate":
                command = new AddBusinessAssociate(commandInput);
                break;
            case "changeSpendingLimit":
                command = new ChangeLimit(commandInput, output, true);
                break;
            case "changeDepositLimit":
                command = new ChangeLimit(commandInput, output, false);
                break;
            case "businessReport":
                command = new BusinessReport(commandInput, output);
                break;
            case "cashWithdrawal":
                command = new CashWithdrawal(commandInput, output);
                break;
            case "acceptSplitPayment", "rejectSplitPayment":
                command = new AcceptRejectPayment(commandInput, output);
                break;
            default:
                break;
        }

        if (command != null) {
            command.execute();
        }
    }
}
