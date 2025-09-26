package ufrn.pd.service.booking;

import ufrn.pd.client.Client;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.server.Server;
import ufrn.pd.service.Service;
import ufrn.pd.service.ServiceNode;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.utils.protocol.ResponseStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// TODO : Bring the general structure up to the ServiceNode abstract class
public class BookingService implements Service, ServiceNode {

    private final Server server;
    private final Client client;
    private final NodeAddress gatewayAddress;
    private final NodeAddress thisNodeAddress;

    public BookingService(Server server, Client client, NodeAddress gatewayAddress, NodeAddress thsiNodeAddress) {
        this.server = server;
        this.client = client;
        this.gatewayAddress = gatewayAddress;
        this.thisNodeAddress = thsiNodeAddress;
    }


    @Override
    public ResponsePayload handle(RequestPayload request) {
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
        // STUB
        System.out.printf("Node : %s - Mensagem recebida:%n%s", thisNodeAddress, request);
        return null;
    }

    private ResponsePayload handleCreate(String value) {
        String[] values = value.split(":");

        String user = values[0];
        String roomId = values[1];
        String slot = values[2];
        ResponsePayload responsePayload = new ResponsePayload(ResponseStatus.OK, String.format("Booking created for user %s in room %s at slot %s",
                user, roomId, slot), thisNodeAddress);
        System.out.println(responsePayload);
        return responsePayload;
    }

    private ResponsePayload handleHeartbeat(RequestPayload request) {
        return new ResponsePayload(ResponseStatus.OK,
                "Heartbeat received", gatewayAddress);
    }

    @Override
    public boolean raise() {
        int numOfAttempts = 5;
        for (int i = 0; i < numOfAttempts; i++) {
            RequestPayload registerRequestPayload = new RequestPayload(gatewayAddress, NodeRole.BOOKING, NodeRole.GATEWAY,
                    "REGISTER", thisNodeAddress.toString());
            // TODO : Chamada ao servidor
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<ResponsePayload> future = executorService.submit(() -> client.sendAndReceive(gatewayAddress.ip(),
                        gatewayAddress.port(), registerRequestPayload));
                try {
                    ResponsePayload response = future.get(500, TimeUnit.MILLISECONDS);
                    if (response != null) {
                        System.out.println("Recebida resposta do register" + response.status() + response.value());
                        return true;
                    }
                } catch (Exception e ) {
                    System.err.println("BookingService - raise : an exception has occurred : \n" + e.getMessage());
                    System.err.println("Trying again");
                    continue;
                }

            }
        }
        return false;
    }

    public void run() {
        server.runServer(this);
    }
}
