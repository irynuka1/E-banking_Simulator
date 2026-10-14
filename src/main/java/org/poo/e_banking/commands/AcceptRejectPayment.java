package org.poo.e_banking.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.e_banking.AppLogic;
import org.poo.e_banking.helpers.Executable;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

public final class AcceptRejectPayment implements Executable {
    private final CommandInput commandInput;
    private final ArrayNode output;

    public AcceptRejectPayment(final CommandInput commandInput, final ArrayNode output) {
        this.commandInput = commandInput;
        this.output = output;
    }

    @Override
    public void execute() {
        ArrayList<ObjectNode> errorArray = AppLogic.getInstance().getUserNotFounds();

        for (ObjectNode jsonNodes : errorArray) {
            if (jsonNodes.get("timestamp").asInt() == commandInput.getTimestamp()) {
                output.add(jsonNodes);
                return;
            }
        }

        if (!AppLogic.getInstance().getUserMap().containsKey(commandInput.getEmail())) {
            ObjectNode wrapper = new ObjectMapper().createObjectNode();
            wrapper.put("command", commandInput.getCommand());
            ObjectNode error = wrapper.putObject("output");
            error.put("description", "User not found");
            error.put("timestamp", commandInput.getTimestamp());
            wrapper.put("timestamp", commandInput.getTimestamp());
            output.add(wrapper);
        }
    }
}
