package ufrn.pd.gateway;

import ufrn.pd.client.Client;
import ufrn.pd.client.TCPClient;
import ufrn.pd.client.UDPClient;
import ufrn.pd.server.Server;
import ufrn.pd.server.TCPServerSocket;
import ufrn.pd.server.UDPServerSocket;
import ufrn.pd.service.Service;
import ufrn.pd.service.user.dtos.RequestPayload;

import java.util.*;
import java.util.concurrent.*;


public class APIGateway implements Service {

    static enum NodeStatus {
        ALIVE,
        DEAD
    }

    // Table of addresses to the nodes managed by this gateway
    private final Map<NodeAddress, NodeRole> addressTable = new ConcurrentHashMap<>();
    private final Map<NodeAddress, NodeStatus> userNodes = new ConcurrentHashMap<>();
    private final Map<NodeAddress, NodeStatus> bookingNodes = new ConcurrentHashMap<>();
    // Network client instace, for starting communications
    private final Client client;
    private final Server server;
    // Heartbeat executor service
    private final ScheduledExecutorService heartBeatExecutorService = Executors.newScheduledThreadPool(1);
    // The address of this node
    private final NodeAddress gatewayAddress;

    // TODO : Decide if the class should have a server
    public APIGateway(Client client, Server server, NodeAddress gatewayAddress) {
        this.client = client;
        this.server = server;
        this.gatewayAddress = gatewayAddress;
    }

    public void run() {
        activateHeartbeatWorker();
        server.runServer(this);
        shutdownHeartbeatWorker();
    }

    private final Runnable heartbeatWorker = new Runnable() {
        @Override
        public void run() {
            System.out.println("Entrou no heartbeatworker");
            List<NodeAddress> addresses = new ArrayList<>();
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                for (NodeAddress address : addressTable.keySet()) {
                    Future<NodeAddress> future = executorService.submit(() -> sendHeartbeat(address, addressTable.get(address)));
                    try {
                        addresses.add(future.get(400, TimeUnit.MILLISECONDS));
                    } catch (TimeoutException e) {
                        System.out.println("⏱ Node " + address + " TIMEOUT - marcado como DEAD");
                        future.cancel(true);
                    } catch (Exception e) {
                        System.err.println("Erro no heartbeat para " + address + ": " + e.getMessage());
                    }
                }
                addresses.forEach(address -> {
                    // TODO : Replace with logging
                    System.out.println("Node " + address + " marcado como ALIVE");
                    addLivingNode(address, addressTable.get(address));
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
            return new RequestPayload(null, null, null, "ERROR", "");
        }
        return handleServiceRequest(payload);
    }

    private void addLivingNode(NodeAddress nodeAddress, NodeRole nodeRole) {
        Map<NodeAddress, NodeStatus> nodeMap = switch (nodeRole) {
            case NodeRole.USER -> userNodes;
            case NodeRole.BOOKING -> bookingNodes;
            default -> null;
        };
        if (nodeMap == null) {
            return;
        }
        nodeMap.put(nodeAddress, NodeStatus.ALIVE);
    }

    private Optional<NodeAddress> getLivingNode(NodeRole nodeService) {
        // TODO : Tratar excecao
        Map<NodeAddress, NodeStatus> nodeMap = switch (nodeService) {
            case USER -> userNodes;
            case BOOKING -> bookingNodes;
            case GATEWAY -> null;
            case CLIENT -> null;
        };

        return nodeMap.entrySet().stream().
                filter(entry -> entry.getValue() == NodeStatus.ALIVE)
                .findFirst().map(Map.Entry::getKey);
    }

    // ! : Blocking
    private RequestPayload handleServiceRequest(RequestPayload payload) {
        NodeRole service = payload.destinationRole();
        int numOfAttempts = 5;
        for (int i = 0; i < numOfAttempts; i++) {
            // Finds an available node for the requested service
            Optional<NodeAddress> address = getLivingNode(service);
            if (address.isEmpty()) {
                // Sleep for a time period and then try again
                // TODO : Not Ideal
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException("APIGateway - a request handling thread has been interrupted");
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
            var heartbeat = new RequestPayload(address, NodeRole.GATEWAY, serviceRole,
                    "HEARTBEAT", "null");
            var response = client.sendAndReceive(address.ip(), address.port(), heartbeat);
            return NodeAddress.fromString(response.value());
    }

    // 1 - No ja registrado manda mensagem
    private RequestPayload registerNewNode(NodeAddress nodeAddress, NodeRole nodeRole) {
        addressTable.put(nodeAddress, nodeRole);
        addLivingNode(nodeAddress, nodeRole);
        System.out.printf("SUCCESS - Node (%s) registered%n", nodeAddress);
        return new RequestPayload(nodeAddress, NodeRole.GATEWAY, nodeRole, "END", null);
    }

    // Should be in try with resources clause
    public void activateHeartbeatWorker() {
        heartBeatExecutorService.scheduleAtFixedRate(heartbeatWorker, 0, 800, TimeUnit.MILLISECONDS);
        heartBeatExecutorService.scheduleAtFixedRate(() -> {
            try {
                heartbeatWorker.run();
            } catch (Exception e) { // This catches everything, to prevent the ScheduledExecutor from shutting down
                System.err.println("heartbeatWorker - An exception has occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 800, TimeUnit.MILLISECONDS);
    }

    public void shutdownHeartbeatWorker() {
        System.out.println("Shutdown heartbeat worker");
        heartBeatExecutorService.shutdown();
    }

    public static void main(String[] args) {
        NodeAddress gatewayAddress = new NodeAddress("localhost", 3001);
        APIGateway apiGateway = new APIGateway(new UDPClient(new PDGatewayProtocol()),
                new Server(new UDPServerSocket(gatewayAddress.port(), 1024), new PDGatewayProtocol()), gatewayAddress);
        apiGateway.run();
    }
}
