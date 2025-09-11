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
import ufrn.pd.service.user.protocol.ResponseStatus;

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
        // TODO : For now, the message structure is "IP\nPORT\nROLE\nOPERATION\nvalue"
        // TODO : Wrap the operations into enums for each service + gateway
        if (request.operation().equalsIgnoreCase("HEARTBEAT")) {
            return handleHeartbeat(request);
        }
        // STUB
        System.out.printf("Node : %s - Mensagem recebida:%n%s", thisNodeAddress, request);
        return null;
    }

    private ResponsePayload handleHeartbeat(RequestPayload request) {
        ResponsePayload heartbeatResponse = new ResponsePayload(ResponseStatus.OK,
                "Heartbeat received", gatewayAddress);
        System.out.println("Retornando HeartBeat");
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
