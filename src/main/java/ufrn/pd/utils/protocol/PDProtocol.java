package ufrn.pd.utils.protocol;



import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;

import java.util.List;
import java.util.Optional;

public abstract class PDProtocol implements ApplicationProtocol {

//    abstract public Optional<RequestPayload> validateMessage(List<String> msg);

//    abstract RequestPayload createErrorMessage(List<String> msg);
    @Override
    public  RequestPayload parseRequest(String message) {
//        System.out.println("Mensagem recebida : " + message);
        List<String> msg = List.of(message.split("\r\n"));
//        System.out.println("Mensagem splitada : " + msg);
        // TODO : Split the validation and error payload creation
        Optional<RequestPayload> errorPayload = validateMessage(message);
        if  (errorPayload.isPresent()) {
            return errorPayload.get();
        }
        String destinationAddressLine = msg.get(1);
        // TODO: use substring lookup
//        System.out.println("DestinationAddressLine: " + destinationAddressLine);
        NodeRole senderRole = NodeRole.valueOf(msg.get(2));
//        System.out.println("NodeRole : " + msg.get(2));
        NodeAddress destinationAddress = NodeAddress.fromString(destinationAddressLine);
        NodeRole destinationRole = NodeRole.valueOf(msg.get(3));
        String operation = msg.get(0);
        return new RequestPayload(destinationAddress, senderRole, destinationRole, operation, msg.get(4));
    }

    // Volta
    @Override
    public  ResponsePayload parseResponse(String message) {
        List<String> msg = List.of(message.split("\n"));
        ResponseStatus status = null;
        try {
            status = ResponseStatus.valueOf(msg.get(0));
        } catch (IllegalArgumentException e) {
            return new ResponsePayload(ResponseStatus.ERROR, "The received operation is status is not valid", null);
        }
        NodeAddress senderAddressLine = NodeAddress.fromString(msg.get(1));
        return new ResponsePayload(status, msg.get(2), senderAddressLine);
    }

    @Override
    public String createRequest(RequestPayload message) {
        return String.format("%s\r\n%s\r\n%s\r\n%s\r\n%s",
                message.operation(),
                message.destinationAddress(),
                message.senderRole(),
                message.destinationRole(),
                message.value());
    }

    @Override
    public String createResponse(ResponsePayload message) {
        return String.format("%s\n%s\n%s\n",message.status().toString(),
                message.senderAddress(), message.value());
    }
}
