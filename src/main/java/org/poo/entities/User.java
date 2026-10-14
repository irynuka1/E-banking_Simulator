package org.poo.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.e_banking.commands.splitPayment.PendingTransaction;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter@Setter
public final class User implements ToOutput {
    private String firstName;
    private String lastName;
    private String email;
    private String birthdate;
    private String occupation;
    private String plan;
    private int transactionsToUpgrade;
    private List<Account> accounts;
    private ObjectNode userNode;
    private ArrayNode transactionsNode;
    private Map<String, Account> accountMap = new HashMap<>();
    private Map<String, Account> associatedAccounts = new HashMap<>();
    private List<PendingTransaction> pendingTransactions = new ArrayList<>();

    public User(final String firstName, final String lastName, final String email,
                final String birthdate, final String occupation) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthdate = birthdate;
        this.occupation = occupation;
        this.transactionsToUpgrade = 0;
        this.plan = this.occupation.equals("student") ? "student" : "standard";
        this.accounts = new ArrayList<>();
        this.userNode = new ObjectNode(new ObjectMapper().getNodeFactory());
        userNode.put("command", "printTransactions");
        transactionsNode = userNode.putArray("output");
    }

    /**
     * This method is used to get the age of the user.
     * @param userBirthdate The birthdate of the user.
     * @return Returns the age of the user.
     */
    public int getUserAge(final String userBirthdate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dateOfBirth = LocalDate.parse(userBirthdate, formatter);

        LocalDate now = LocalDate.now();
        return Period.between(dateOfBirth, now).getYears();
    }

    /**
     * This method is used to add an account to the user.
     * @param accountType The type of the account.
     * @param currency The currency of the account.
     * @param interestRate The interest rate of the account.
     */
    public void addAccount(final String accountType, final String currency,
                           final double interestRate) {
        Account account = AccountFactory.createAccount(accountType, this.email, currency,
                interestRate);
        if (account != null) {
            accounts.add(account);
            accountMap.put(account.getIban(), account);
        }
    }

    /**
     * Adds an already created account to the user.
     * @param account The account to add.
     */
    public void addAccount(final Account account) {
        accounts.add(account);
        accountMap.put(account.getIban(), account);
    }

    /**
     * Gives this user access to an account owned by someone else (business associate).
     * The account is not listed among the user's own accounts.
     * @param account The business account.
     */
    public void addAssociatedAccount(final Account account) {
        associatedAccounts.put(account.getIban(), account);
    }

    /**
     * Gets an account the user can operate on: an owned one or a business account
     * the user is associated with.
     * @param iban The IBAN of the account.
     * @return Returns the account if it exists, null otherwise.
     */
    public Account getAccessibleAccountByIban(final String iban) {
        Account account = accountMap.get(iban);
        return account != null ? account : associatedAccounts.get(iban);
    }

    /**
     * This method is used to remove an account from the user.
     * @param iban The IBAN of the account.
     * @return Returns true if the account was removed, false otherwise.
     */
    public boolean removeAccount(final String iban) {
        if (accountMap.containsKey(iban)) {
            Account accountToRemove = accountMap.get(iban);
            if (accountToRemove.getBalance() == 0) {
                accounts.remove(accountToRemove);
                accountMap.remove(iban);
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to get an account by its IBAN.
     * @param iban The IBAN of the account.
     * @return Returns the account if it exists, null otherwise.
     */
    public Account getAccountByIban(final String iban) {
        return accountMap.get(iban);
    }

    /**
     * This method is used to get an account by its alias.
     * @param alias The alias of the account.
     * @return Returns the account if it exists, null otherwise.
     */
    public Account getAccountByAlias(final String alias) {
        for (Account account : accounts) {
            if (account.getAlias().equals(alias)) {
                return account;
            }
        }
        return null;
    }

    /**
     * This method is used to add funds to an account.
     * @param iban The IBAN of the account.
     * @param amount The amount to be added.
     * @return Returns true if the funds were added, false otherwise.
     */
    public boolean addFundsToAccount(final String iban, final double amount) {
        Account account = getAccountByIban(iban);
        if (account != null) {
            account.addFunds(amount);
            return true;
        }

        return false;
    }

    /**
     * This method is used to add a card to an account.
     * @param iban The IBAN of the account.
     * @param cardType The type of the card.
     */
    public void addCardToAccount(final String iban, final String cardType) {
        Account account = getAccountByIban(iban);
        if (account != null) {
            account.addCard(cardType);
        }
    }

    /**
     * This method gets a card by its number.
     * @param cardNumber The number of the card.
     * @return Returns the card if it exists, null otherwise.
     */
    public Card getCardByNumber(final String cardNumber) {
        for (Account account : accounts) {
            if (account.getCardMap().containsKey(cardNumber)) {
                return account.getCardMap().get(cardNumber);
            }
        }
        for (Account account : associatedAccounts.values()) {
            if (account.getCardMap().containsKey(cardNumber)) {
                return account.getCardMap().get(cardNumber);
            }
        }
        return null;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode user = new ObjectNode(new ObjectMapper().getNodeFactory());
        user.put("firstName", this.firstName);
        user.put("lastName", this.lastName);
        user.put("email", this.email);
        user.putArray("accounts");

        ArrayNode accountsNode = user.putArray("accounts");
        for (Account account : accounts) {
            accountsNode.add(account.toJson());
        }

        return user;
    }
}
