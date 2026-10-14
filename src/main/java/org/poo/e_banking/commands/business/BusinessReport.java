package org.poo.e_banking.commands.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.Executable;
import org.poo.entities.Account;
import org.poo.entities.BusinessAccount;
import org.poo.entities.BusinessActivity;
import org.poo.entities.User;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BusinessReport implements Executable {
    private final CommandInput commandInput;
    private final ArrayNode output;

    public BusinessReport(final CommandInput commandInput, final ArrayNode output) {
        this.commandInput = commandInput;
        this.output = output;
    }

    @Override
    public void execute() {
        Account account = BusinessSupport.findAccount(commandInput.getAccount());
        if (account == null) {
            BusinessSupport.error(output, commandInput.getCommand(), "Account not found",
                    commandInput.getTimestamp());
            return;
        }
        if (!(account instanceof BusinessAccount business)) {
            BusinessSupport.error(output, commandInput.getCommand(),
                    "Account is not of type business", commandInput.getTimestamp());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.put("command", commandInput.getCommand());
        ObjectNode report = wrapper.putObject("output");
        report.put("IBAN", business.getIban());
        report.put("balance", business.getBalance());
        report.put("currency", business.getCurrency());
        report.put("spending limit", business.getSpendingLimit());
        report.put("deposit limit", business.getDepositLimit());
        report.put("statistics type", commandInput.getType());

        List<BusinessActivity> activities = activitiesInRange(business);
        if ("commerciant".equals(commandInput.getType())) {
            fillCommerciants(report, business, activities);
        } else {
            fillTransactions(report, business, activities);
        }

        wrapper.put("timestamp", commandInput.getTimestamp());
        output.add(wrapper);
    }

    private List<BusinessActivity> activitiesInRange(final BusinessAccount business) {
        List<BusinessActivity> result = new ArrayList<>();
        for (BusinessActivity activity : business.getActivities()) {
            if (activity.getTimestamp() >= commandInput.getStartTimestamp()
                    && activity.getTimestamp() <= commandInput.getEndTimestamp()) {
                result.add(activity);
            }
        }
        return result;
    }

    private String username(final String email) {
        User user = AppLogic.getInstance().getUserMap().get(email);
        return user.getLastName() + " " + user.getFirstName();
    }

    private void fillTransactions(final ObjectNode report, final BusinessAccount business,
                                  final List<BusinessActivity> activities) {
        double totalSpent = 0;
        double totalDeposited = 0;
        ArrayNode managers = report.putArray("managers");
        ArrayNode employees = report.putArray("employees");

        for (Map.Entry<String, String> associate : business.getAssociates().entrySet()) {
            double spent = 0;
            double deposited = 0;
            for (BusinessActivity activity : activities) {
                if (activity.getEmail().equals(associate.getKey())) {
                    if (activity.isDeposit()) {
                        deposited += activity.getAmount();
                    } else {
                        spent += activity.getAmount();
                    }
                }
            }
            totalSpent += spent;
            totalDeposited += deposited;

            ObjectNode entry = new ObjectMapper().createObjectNode();
            entry.put("username", username(associate.getKey()));
            entry.put("spent", spent);
            entry.put("deposited", deposited);
            if (associate.getValue().equals(BusinessAccount.MANAGER)) {
                managers.add(entry);
            } else {
                employees.add(entry);
            }
        }

        report.put("total spent", totalSpent);
        report.put("total deposited", totalDeposited);
    }

    private void fillCommerciants(final ObjectNode report, final BusinessAccount business,
                                  final List<BusinessActivity> activities) {
        Map<String, List<BusinessActivity>> byCommerciant = new TreeMap<>();
        for (BusinessActivity activity : activities) {
            if (!activity.isDeposit() && activity.getCommerciant() != null) {
                byCommerciant.computeIfAbsent(activity.getCommerciant(), k -> new ArrayList<>())
                        .add(activity);
            }
        }

        ArrayNode commerciants = report.putArray("commerciants");
        for (Map.Entry<String, List<BusinessActivity>> entry : byCommerciant.entrySet()) {
            double total = 0;
            ArrayNode managers = new ObjectMapper().createArrayNode();
            ArrayNode employees = new ObjectMapper().createArrayNode();
            for (BusinessActivity activity : entry.getValue()) {
                total += activity.getAmount();
                String name = username(activity.getEmail());
                if (BusinessAccount.MANAGER.equals(business.getRole(activity.getEmail()))) {
                    managers.add(name);
                } else {
                    employees.add(name);
                }
            }

            ObjectNode node = commerciants.addObject();
            node.put("commerciant", entry.getKey());
            node.put("total received", total);
            node.set("managers", managers);
            node.set("employees", employees);
        }
    }
}
