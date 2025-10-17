package ufrn.pd.service.bm25service;

import ufrn.pd.client.GRPCClient;
import ufrn.pd.client.TCPClient;
import ufrn.pd.client.UDPClient;
import ufrn.pd.gateway.NetworkStack;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.server.*;
import ufrn.pd.service.bm25service.protocol.HTTPUserProtocol;
import ufrn.pd.service.bm25service.protocol.PDUserProtocol;

public class BM25ServiceMain {
    public static void main(String[] args) {
        String net = null;
        int thisPort = 0;
        int gatewayPort = 0;
        if (args.length > 0) {
            gatewayPort = Integer.parseInt(args[1]);
            thisPort = Integer.parseInt(args[2]);
            net = args[0].toLowerCase();
        } else {
            gatewayPort = 3001;
            thisPort = 3003;
            net = "udp";
        }

        NodeAddress gatewayAddress = new NodeAddress("localhost", gatewayPort);
        NodeAddress thisNodeAddress = new NodeAddress("localhost", thisPort);
        NetworkStack netStack = switch (net) {
            case "tcp" -> new NetworkStack(new TCPClient(new HTTPUserProtocol()),
                    new ServerImpl(new TCPServerSocket(thisNodeAddress.port(), 1000), new HTTPUserProtocol()));
            case "udp" -> new NetworkStack(new UDPClient(new PDUserProtocol()),
                    new ServerImpl(new UDPServerSocket(thisNodeAddress.port(), 4250), new PDUserProtocol()));
            case "grpc" -> new NetworkStack(new GRPCClient(), new GRPCServerImpl(thisNodeAddress.port()));
            default -> throw new IllegalArgumentException("Invalid protocol");
        };
        BM25Service userService = new BM25Service(gatewayAddress, thisNodeAddress, netStack.client(), netStack.server());
        userService.run();
    }
}
