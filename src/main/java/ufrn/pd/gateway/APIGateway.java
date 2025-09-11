package ufrn.pd.gateway;

import ufrn.pd.client.Client;
import ufrn.pd.server.Server;
import ufrn.pd.service.Service;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.service.user.protocol.ResponseStatus;

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
            List<NodeAddress> addresses = new ArrayList<>();
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                for (NodeAddress address : addressTable.keySet()) {
                    NodeStatus nodeStatus = NodeStatus.ALIVE;
                    Future<NodeAddress> future = executorService.submit(() -> sendHeartbeat(address, addressTable.get(address)));
                    try {
                        addresses.add(future.get(400, TimeUnit.MILLISECONDS));
                    } catch (TimeoutException e) {
                        future.cancel(true);
                        nodeStatus = NodeStatus.DEAD;
                    } catch (Exception e) {
                        e.printStackTrace();
                        nodeStatus = NodeStatus.DEAD;
                    }
                    System.out.println("Node " + address + " marcado como " + nodeStatus);
                    updateNodeStatus(address, addressTable.get(address), nodeStatus);
                }
            }
        }
    };

    // TODO : Erros na formatacao da mensagem serao tratadas na camada protocol
    @Override
    public ResponsePayload handle(RequestPayload payload) {
//        Non functional requests
        if (Objects.equals(payload.operation(), "REGISTER")) {
            NodeAddress senderAddress = NodeAddress.fromString(payload.value());
            return registerNewNode(senderAddress, payload.senderRole());
        }
        // TODO : How to check if a service request is indeed a part of the addressTable
        if (!addressTable.containsKey(payload.destinationAddress())) {
            // TODO : Response
            return new ResponsePayload(ResponseStatus.FORBIDDEN, "Unknown service client", gatewayAddress);
        }
        return handleServiceRequest(payload);
    }

    private void updateNodeStatus(NodeAddress nodeAddress, NodeRole nodeRole, NodeStatus newStatus) {
        Map<NodeAddress, NodeStatus> nodeMap = switch (nodeRole) {
            case NodeRole.USER -> userNodes;
            case NodeRole.BOOKING -> bookingNodes;
            default -> null;
        };
        if (nodeMap == null) {
            return;
        }
        nodeMap.put(nodeAddress, newStatus);
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

    private ResponsePayload handleServiceRequest(RequestPayload payload) {
        NodeRole service = payload.destinationRole();
        int numOfAttempts = 5;
        for (int i = 0; i < numOfAttempts; i++) {
            // Finds an available node for the requested service
            Optional<NodeAddress> address = getLivingNode(service);
            if (address.isEmpty()) {
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
            ResponsePayload serviceResponse = client.sendAndReceive(chosenNodeAddress.ip(), chosenNodeAddress.port(), messageToService);
            return new ResponsePayload(serviceResponse.status(), serviceResponse.value(), gatewayAddress);
        }
//        return "ERROR - No available nodes"; // TODO: Replace with a protocol method (return 500 or 312 for http)
        return null; // TODO: Replace with a protocol method (return 500 or 312 for http)

    }

    // TODO : When the protocol goes to the server, this method wont exist anymore
    private NodeAddress sendHeartbeat(NodeAddress address, NodeRole serviceRole) {
        var heartbeat = new RequestPayload(address, NodeRole.GATEWAY, serviceRole,
                "HEARTBEAT", "pending");
        ResponsePayload response = client.sendAndReceive(address.ip(), address.port(), heartbeat);
        if (response.status() == ResponseStatus.OK) {
            return address;
        }
        return null;
    }

    // 1 - No ja registrado manda mensagem
    private ResponsePayload registerNewNode(NodeAddress nodeAddress, NodeRole nodeRole) {
        addressTable.put(nodeAddress, nodeRole);
        updateNodeStatus(nodeAddress, nodeRole, NodeStatus.ALIVE);
        System.out.printf("SUCCESS - Node (%s) registered%n", nodeAddress);
        // TODO : response
        return new ResponsePayload(ResponseStatus.OK, "success", gatewayAddress);
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
}