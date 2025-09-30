package ufrn.pd.service.user;

import ufrn.pd.client.GRPCClient;
import ufrn.pd.client.TCPClient;
import ufrn.pd.client.UDPClient;
import ufrn.pd.gateway.NetworkStack;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.server.*;
import ufrn.pd.service.user.protocol.HTTPUserProtocol;
import ufrn.pd.service.user.protocol.PDUserProtocol;

public class UserServiceMain {
    public static void main(String[] args) {
        int gatewayPort = Integer.parseInt(args[1]);
        int thisPort = Integer.parseInt(args[2]);
        NodeAddress gatewayAddress = new NodeAddress("localhost", gatewayPort);
        NodeAddress thisNodeAddress = new NodeAddress("localhost", thisPort);
        String net = args[0].toLowerCase();
        NetworkStack netStack = switch (net) {
            case "tcp" -> new NetworkStack(new TCPClient(new HTTPUserProtocol()),
                    new ServerImpl(new TCPServerSocket(thisNodeAddress.port(), 1000),new HTTPUserProtocol()));
            case "udp" -> new NetworkStack(new UDPClient(new PDUserProtocol()),
                    new ServerImpl(new UDPServerSocket(thisNodeAddress.port(), 1000),new PDUserProtocol()));
            case "grpc" -> new NetworkStack(new GRPCClient(), new GRPCServerImpl(thisNodeAddress.port()));
            default -> throw new IllegalArgumentException("Invalid protocol");
        };
        UserService userService = new UserService(gatewayAddress, thisNodeAddress, netStack.client(), netStack.server());
        if(!userService.raise()) {
            System.err.println("O no nao foi registrado");
        }
        userService.run();
    }
}
