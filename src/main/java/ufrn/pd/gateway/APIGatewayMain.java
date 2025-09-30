package ufrn.pd.gateway;

import ufrn.pd.client.Client;
import ufrn.pd.client.GRPCClient;
import ufrn.pd.client.TCPClient;
import ufrn.pd.client.UDPClient;
import ufrn.pd.gateway.protocol.HTTPGatewayProtocol;
import ufrn.pd.gateway.protocol.PDGatewayProtocol;
import ufrn.pd.server.*;

public class APIGatewayMain {
    public static void main(String[] args) {
        String net = args[0].toLowerCase();
        String port = args[1].toLowerCase();
        NodeAddress gatewayAddress = new NodeAddress("localhost", Integer.parseInt(port));
        NetworkStack netStack = switch (net) {
            case "tcp" -> new NetworkStack(new TCPClient(new HTTPGatewayProtocol()),
                    new ServerImpl(new TCPServerSocket(gatewayAddress.port(), 1000),new HTTPGatewayProtocol()));
            case "udp" -> new NetworkStack(new UDPClient(new PDGatewayProtocol()),
                    new ServerImpl(new UDPServerSocket(gatewayAddress.port(), 1000),new PDGatewayProtocol()));
            case "grpc" -> new NetworkStack(new GRPCClient(), new GRPCServerImpl(gatewayAddress.port()));
            default -> throw new IllegalArgumentException("Invalid protocol");
        };
        APIGateway apiGateway = new APIGateway(netStack.client(), netStack.server(), gatewayAddress);
        apiGateway.run();
    }
}
