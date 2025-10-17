package ufrn.pd.service.datastore;

import ufrn.pd.client.GRPCClient;
import ufrn.pd.client.TCPClient;
import ufrn.pd.client.UDPClient;
import ufrn.pd.gateway.NetworkStack;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.server.*;
import ufrn.pd.service.datastore.protocol.HTTPBookingProtocol;
import ufrn.pd.service.datastore.protocol.PDBookingProtocol;
import ufrn.pd.service.bm25service.protocol.PDUserProtocol;

public class BM25DataStoreMain {
    public static void main(String[] args) {
        String net = null;
        int gatewayPort = 0;
        int thisPort = 0;
        if (args.length > 0) {
            net = args[0].toLowerCase();
            gatewayPort = Integer.parseInt(args[1]);
            thisPort = Integer.parseInt(args[2]);
        } else {
            net = "udp";
            gatewayPort = 3001;
            thisPort = 3002;
        }

        NodeAddress gatewayAddress = new NodeAddress("localhost", gatewayPort);
        NodeAddress thisNodeAddress = new NodeAddress("localhost", thisPort);
        NetworkStack netStack = switch (net) {
            case "tcp" -> new NetworkStack(new TCPClient(new HTTPBookingProtocol()),
                    new ServerImpl(new TCPServerSocket(thisNodeAddress.port(), 1000), new HTTPBookingProtocol()));
            case "udp" -> new NetworkStack(new UDPClient(new PDUserProtocol()),
                    new ServerImpl(new UDPServerSocket(thisNodeAddress.port(), 4250), new PDBookingProtocol()));
            case "grpc" -> new NetworkStack(new GRPCClient(), new GRPCServerImpl(thisNodeAddress.port()));
            default -> throw new IllegalArgumentException("Invalid protocol");
        };
        BM25DataStore bm25DataStore = new BM25DataStore(netStack.server(), netStack.client(), gatewayAddress, thisNodeAddress
        );
        bm25DataStore.run();
        if (!bm25DataStore.raise()) {
            System.out.println("O no Data Store nao foi registrado");

        }
    }
}