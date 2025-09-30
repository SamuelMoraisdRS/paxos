package ufrn.pd.service.booking;

import ufrn.pd.client.Client;
import ufrn.pd.client.GRPCClient;
import ufrn.pd.client.TCPClient;
import ufrn.pd.client.UDPClient;
import ufrn.pd.gateway.APIGatewayMain;
import ufrn.pd.gateway.NetworkStack;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.server.*;
import ufrn.pd.service.booking.protocol.HTTPBookingProtocol;
import ufrn.pd.service.booking.protocol.PDBookingProtocol;
import ufrn.pd.service.user.protocol.PDUserProtocol;

public class BookingServiceMain {
    public static void main(String[] args) {
        String net = args[0].toLowerCase();
        int gatewayPort = Integer.parseInt(args[1]);
        int thisPort= Integer.parseInt(args[2]);
        NodeAddress gatewayAddress = new NodeAddress("localhost", gatewayPort);
        NodeAddress thisNodeAddress = new NodeAddress("localhost", thisPort);
        NetworkStack netStack = switch (net) {
            case "tcp" -> new NetworkStack(new TCPClient(new HTTPBookingProtocol()),
                    new ServerImpl(new TCPServerSocket(thisNodeAddress.port(), 1000), new HTTPBookingProtocol()));
            case "udp" -> new NetworkStack(new UDPClient(new PDUserProtocol()),
                    new ServerImpl(new UDPServerSocket(thisNodeAddress.port(), 1000), new PDBookingProtocol()));
            case "grpc" -> new NetworkStack(new GRPCClient(), new GRPCServerImpl(thisNodeAddress.port()));
            default -> throw new IllegalArgumentException("Invalid protocol");
        };
        BookingService bookingService = new BookingService(netStack.server(), netStack.client(),gatewayAddress,thisNodeAddress
                );
        if (bookingService.raise()) {
            bookingService.run();
        }
    }
}