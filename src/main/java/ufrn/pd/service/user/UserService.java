package ufrn.pd.service.user;

// Should i put the response message status on the value field of the RequestPayload
// Pros : No need to modify the interfaces
// cons : Might be confunsing, delegates parsing to the service layer in a way

import ufrn.pd.client.Client;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.server.Server;
import ufrn.pd.service.Service;
import ufrn.pd.service.ServiceNode;
import ufrn.pd.utils.protocol.ResponseStatus;

public class UserService implements Service, ServiceNode {
    private final NodeAddress gatewayAddress;
    private final NodeAddress thisNodeAddress;
    private final Client client;
    private final Server server;

    public UserService(NodeAddress gatewayAddress, NodeAddress thisNodeAddress, Client client, Server server) {
        this.gatewayAddress = gatewayAddress;
        this.thisNodeAddress = thisNodeAddress;
        this.client = client;
        this.server = server;
    }

    // TODO : WHen the protocol layer is attached to the server, this method will receive request payloads and multiplex
    // them to the appropriate handle function
    @Override
    public ResponsePayload handle(RequestPayload request) {
        System.out.println("Operation" + request.operation());
        if (request.operation().equalsIgnoreCase("ERROR")) {
            return new ResponsePayload(ResponseStatus.ERROR, request.value(), thisNodeAddress);
        }
        // TODO : Wrap the operations into enums for each service + gateway
        if (request.operation().equalsIgnoreCase("HEARTBEAT")) {
            return handleHeartbeat(request);
        }
        if (request.operation().equalsIgnoreCase("CREATE")) {
            return handleCreate(request.value());
        }
        if (request.operation().equalsIgnoreCase("CALCULATE")) {
            return handleCalculateScore(request.value());
        }
        System.out.println("CHEGOU NO NULL");

        return new ResponsePayload(ResponseStatus.ERROR,
                "Unknown operation: " + request.operation(), thisNodeAddress);
//        return null;
    }

    private ResponsePayload handleCreate(String userValue) {
        String [] values = userValue.split(":");
        // The deserialization should be done on the protocol layer
        String userName = values[0];
        String score = values[1];
        ResponsePayload responsePayload = new ResponsePayload(ResponseStatus.OK,
                String.format("User Created - Name : %s , Score : %s", userName, score), thisNodeAddress);
        System.out.println(responsePayload);
        return responsePayload;
    }

    private ResponsePayload handleCalculateScore(String calculateScoreValue) {
        String [] values = calculateScoreValue.split(":");
        // The deserialization should be done on the protocol layer
        String name = values[0];
        Integer val0 = Integer.parseInt(values[1]);
        Integer val1 = Integer.parseInt(values[2]);
        Integer val2 = Integer.parseInt(values[3]);
        ResponsePayload responsePayload = new ResponsePayload(ResponseStatus.OK,
                String.format("User %s has a score of %d", name, val0 + val1 + val2), thisNodeAddress);
        System.out.println(responsePayload);
        return responsePayload;

    }
    private ResponsePayload handleHeartbeat(RequestPayload request) {
        ResponsePayload heartbeatResponse = new ResponsePayload(ResponseStatus.OK,
                "Heartbeat received", gatewayAddress);
//        System.out.println("Retornando HeartBeat");
        return heartbeatResponse;

    }

    public void run() {
        server.runServer(this);
    }

    @Override
    public boolean raise() {
        int numOfAttempts = 5;
        RequestPayload registerRequestPayload = new RequestPayload(gatewayAddress, NodeRole.USER, NodeRole.GATEWAY, "REGISTER", thisNodeAddress.toString());
        // TODO : Chamada ao servidor
        ResponsePayload response  = client.sendAndReceive(gatewayAddress.ip(), gatewayAddress.port(), registerRequestPayload);
        System.out.println("Recebida resposta do register" + response.status() + response.value());
        return true;
    }
}
