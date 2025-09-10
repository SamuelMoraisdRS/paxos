package ufrn.pd.utils.protocol;



import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.user.dtos.RequestPayload;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;

public abstract class PDProtocol implements ApplicationProtocol {

    abstract public Optional<RequestPayload> validateMessage(List<String> msg);

//    abstract RequestPayload createErrorMessage(List<String> msg);
    @Override
    public  RequestPayload parse(String message) {
        List<String> msg = List.of(message.split("\n"));
        // TODO : Split the validation and error payload creation
        Optional<RequestPayload> errorPayload = validateMessage(msg);
        if  (errorPayload.isPresent()) {
            return errorPayload.get();
        }
        String destinationAddressLine = msg.get(1);
        // TODO: use substring lookup
        NodeAddress destinationAddress = NodeAddress.fromString(destinationAddressLine);
        NodeRole senderRole = NodeRole.valueOf(msg.get(2));
        NodeRole destinationRole = NodeRole.valueOf(msg.get(3));
        String operation = msg.get(0);
        return new RequestPayload(destinationAddress, senderRole, destinationRole, operation, msg.get(4));
    }


    @Override
    public String createMessage(RequestPayload message) {
        return String.format("%s%n%s%n%s%n%s%n%s%n",message.operation(),
                message.destinationAddress(), message.senderRole(), message.destinationRole(), message.value());
    }
}
