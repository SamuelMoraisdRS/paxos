package ufrn.pd.service.user.dtos;

import ufrn.pd.client.Client;
import ufrn.pd.client.TCPClient;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.server.Server;
import ufrn.pd.server.TCPServerSocket;
import ufrn.pd.service.Service;
import ufrn.pd.service.ServiceNode;
import ufrn.pd.utils.protocol.ApplicationProtocol;

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
    public RequestPayload handle(RequestPayload request) {
        // TODO : For now, the message structure is "IP\nPORT\nROLE\nOPERATION\nvalue"
        // TODO : Wrap the operations into enums for each service + gateway
        if (request.operation().equalsIgnoreCase("HEARTBEAT")) {
            return handleHeartbeat(request);
        }
        // STUB
        System.out.printf("Node : %s - Mensagem recebida:%n%s", thisNodeAddress, request);
        return null;
    }

    private RequestPayload handleHeartbeat(RequestPayload request) {
        RequestPayload heartbeatResponse = new RequestPayload(gatewayAddress, NodeRole.USER, NodeRole.GATEWAY,
                "END", thisNodeAddress.toString());
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
        RequestPayload response  = client.sendAndReceive(gatewayAddress.ip(), gatewayAddress.port(), registerRequestPayload);
        System.out.println("Recebida resposta do register" + response.operation());
        return true;
    }
    public static void main(String[] args) {
        NodeAddress gatewayAddress = new NodeAddress("localhost", 3001);
        NodeAddress thisNodeAddress = new NodeAddress("localhost", 3002);
        UserService service = new UserService(gatewayAddress, thisNodeAddress, new TCPClient(new ApplicationProtocol()),
                new Server(new TCPServerSocket(thisNodeAddress.port(), 100), new ApplicationProtocol()));
        if(!service.raise()) {
            System.err.println("O no nao foi registrado");
        }
        service.run();
    }
}
