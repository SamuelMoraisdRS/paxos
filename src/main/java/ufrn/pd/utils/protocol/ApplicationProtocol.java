package ufrn.pd.utils.protocol;

import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.ResponsePayload;
import ufrn.pd.service.user.dtos.RequestPayload;

import java.io.IOException;
import java.util.List;

/*
Class that will represent the application's protocol, performing parsing and generating responses
 */
// TODO : Turn this into an abstract class and create the validation and error messages hooks
public class ApplicationProtocol {
    public ApplicationProtocol() {};

    public  boolean validateMessage(List<String> msg) {
//        if (msg.size() < 6) {
//            return false;
//        }
        return true;
    }

//    abstract RequestPayload createErrorMessage(List<String> msg);

    public  RequestPayload parse(String message) throws IOException {
        List<String> msg = List.of(message.split("\n"));
        if  (!validateMessage(msg)) {
            // TODO : Exception handling
            return null;
        }
        System.out.printf("AppProtocol - message : %s%n", message);
        System.out.printf("AppProtocol - msg list : %s%n", msg);
        String destinationAddressLine = msg.get(1);
        // TODO: use substring lookup
        NodeAddress destinationAddress = NodeAddress.fromString(destinationAddressLine);
        NodeRole senderRole = NodeRole.valueOf(msg.get(2));
        NodeRole destinationRole = NodeRole.valueOf(msg.get(3));
        String operation = msg.get(0);
        return new RequestPayload(destinationAddress, senderRole, destinationRole, operation, msg.get(4));
    }


     public String createMessage(RequestPayload message) {
        return String.format("%s%n%s%n%s%n%s%n%s%n",message.operation(),
                message.destinationAddress(), message.senderRole(), message.destinationRole(), message.value());
    }

}
