package ufrn.pd.gateway;

import ufrn.pd.client.Client;
import ufrn.pd.client.TCPClient;
import ufrn.pd.server.Server;
import ufrn.pd.server.TCPServerSocket;
import ufrn.pd.service.Service;
import ufrn.pd.service.user.dtos.RequestPayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;

import java.util.*;
import java.util.concurrent.*;

public class APIGateway implements Service {

    // Table of addresses to the nodes managed by this gateway
    private final Map<NodeAddress, NodeRole> addressTable = new HashMap<>();
    // TODO : Not scalable
    private final ConcurrentLinkedQueue<NodeAddress> userAvailableNodes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NodeAddress> bookingAvailableNodes = new ConcurrentLinkedQueue<>();
    private final Client client;
    private final Server server;
    private final ScheduledExecutorService heartBeatExecutorService = Executors.newScheduledThreadPool(1);
    private final NodeAddress gatewayAddress;

    // TODO : Decide if the class should have a server
    public APIGateway(Client client, Server server, NodeAddress gatewayAddress) {
        this.client = client;
        this.server = server;
        this.gatewayAddress = gatewayAddress;
    }

    public void run() {
        activateHeartbeatWorker();
        ExecutorService serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
        server.runServer(this);
        // TODO : Delegar a outra thread
        shutdownHeartbeatWorker();
    }

    private final Runnable heartbeatWorker = new Runnable() {
        @Override
        public void run() {
            Set<Future<NodeAddress>> responses = new HashSet<>();
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                for (NodeAddress address : addressTable.keySet()) {
                    responses.add(executorService.submit(() -> sendHeartbeat(address,addressTable.get(address))));
                }
                responses.stream().map(future -> {
                            try {
                                return future.get(400, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                System.err.println("Lancou interrupted");
                                throw new RuntimeException(e);
                            } catch (ExecutionException e) {
                                System.err.println("Lancou execution");
                                System.err.println(e.getMessage());
                                throw new RuntimeException(e);
                            } catch (TimeoutException e) {
                                System.err.println("Lancou timeout");
                                throw new RuntimeException(e);
                            }
                        }).
                        filter(Objects::nonNull).forEach(address -> {
                            enqueueLivingNode(address, addressTable.get(address));
                        });
            }
        }
    };

    // TODO : Erros na formatacao da mensagem serao tratadas na camada protocol
    @Override
    public RequestPayload handle(RequestPayload payload) {
//        Non functional requests
        if (Objects.equals(payload.operation(), "REGISTER")) {
             NodeAddress senderAddress = NodeAddress.fromString(payload.value());
            return registerNewNode(senderAddress, payload.senderRole());
        }
        // TODO : How to check if a service request is indeed a part of the addressTable
        if (!addressTable.containsKey(payload.destinationAddress())) {
            return new RequestPayload(null, null, null, "ERROR","" );
        }
        return handleServiceRequest(payload);
    }

    private void enqueueLivingNode(NodeAddress nodeAddress, NodeRole nodeRole) {
        ConcurrentLinkedQueue<NodeAddress> queue = switch (nodeRole) {
            case NodeRole.USER -> userAvailableNodes;
            case NodeRole.BOOKING -> bookingAvailableNodes;
            default -> throw new IllegalStateException("Unexpected value: " + nodeRole);
        };
        // TODO : BAD
        if (queue.size() < addressTable.size()) {
            queue.add(nodeAddress);
        }
    }

    private Optional<NodeAddress> getLivingNode(NodeRole nodeService) {
        // TODO : Tratar excecao
        NodeAddress address = switch (nodeService) {
            case USER -> userAvailableNodes.poll();
            case BOOKING -> bookingAvailableNodes.poll();
            case GATEWAY -> null;
            case CLIENT -> null;
        };
        return Optional.ofNullable(address);
    }

    // ! : Blocking
    private RequestPayload handleServiceRequest(RequestPayload payload) {
        NodeRole service = payload.destinationRole();
//        NodeAddress clientAddress = payload.senderAddress();
        int numOfAttempts = 5;
        for (int i = 0; i < numOfAttempts; i++) {
            // Find an available node for the requested service
            Optional<NodeAddress> address = getLivingNode(service);

            if (address.isEmpty()) {
                // Sleep for a time period and then try again
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            NodeAddress chosenNodeAddress = address.get();
            RequestPayload messageToService = new RequestPayload(chosenNodeAddress, service, NodeRole.GATEWAY, payload.operation(), payload.value());
            // Creates a connection to the service node, sends the appropriate request to it and captures the response
            RequestPayload serviceResponse = client.sendAndReceive(chosenNodeAddress.ip(), chosenNodeAddress.port(), messageToService);
            // TODO : We cant access the clients address on this layer
            return new RequestPayload(gatewayAddress, service, NodeRole.CLIENT, serviceResponse.operation(), serviceResponse.value());
        }
//        return "ERROR - No available nodes"; // TODO: Replace with a protocol method (return 500 or 312 for http)
        return null; // TODO: Replace with a protocol method (return 500 or 312 for http)

    }

    // TODO : When the protocol goes to the server, this method wont exist anymore
    private NodeAddress sendHeartbeat(NodeAddress address, NodeRole serviceRole) {
        System.out.println("Enviando heartbeat");
        var heartbeat = new RequestPayload(address, NodeRole.GATEWAY, serviceRole,
             "HEARTBEAT", "null");
        try {
            var response = client.sendAndReceive(address.ip(), address.port(), heartbeat);
            return NodeAddress.fromString(response.value());
        } catch (Exception e) {
            System.err.println("Heartbeat - Excecao ao enviar a message");

            System.err.println(e.getMessage());
        }
        return null;
    }

    // 1 - No ja registrado manda mensagem
    private RequestPayload registerNewNode(NodeAddress nodeAddress, NodeRole nodeRole) {
//        if (addressTable.containsKey(nodeAddress)) {
//            return new RequestPayload(nodeAddress, NodeRole.GATEWAY, nodeRole, "END", null );
//        }
        addressTable.put(nodeAddress, nodeRole);
        enqueueLivingNode(nodeAddress, nodeRole);
        System.out.printf("SUCCESS - Node (%s) registered%n", nodeAddress);
        RequestPayload reply = new RequestPayload(nodeAddress, NodeRole.GATEWAY, nodeRole, "END", null );
        return reply;
    }

    // Should be in try with resources clause
    public void activateHeartbeatWorker() {
        heartBeatExecutorService.scheduleAtFixedRate(heartbeatWorker, 0, 800, TimeUnit.MILLISECONDS);
    }

    public void shutdownHeartbeatWorker() {
        heartBeatExecutorService.shutdown();
    }

    public static void main(String[] args) {
        NodeAddress gatewayAddress = new NodeAddress("localhost", 3001);
        APIGateway apiGateway = new APIGateway(new TCPClient(new ApplicationProtocol()),
                new Server(new TCPServerSocket(3001, 100), new ApplicationProtocol()), gatewayAddress);
        apiGateway.run();
    }
}
