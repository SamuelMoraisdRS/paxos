package ufrn.pd.gateway;

import ufrn.pd.client.Client;
import ufrn.pd.server.Server;
import ufrn.pd.service.Service;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.utils.protocol.ResponseStatus;

import java.util.*;
import java.util.concurrent.*;


public class APIGateway implements Service {

    static enum NodeStatus {
        ALIVE,
        DEAD
    }

    // Table of addresses to the nodes managed by this gateway
    private final Map<NodeAddress, NodeRole> addressTable = new ConcurrentHashMap<>();

    // ! : High coupling
    private final ConcurrentHashMap<NodeAddress, NodeStatus> userNodes = new ConcurrentHashMap<>();
    private final Map<NodeAddress, NodeStatus> bookingNodes = new ConcurrentHashMap<>();
//    private final Set<NodeAddress> livingBookingNodesSet = userNodes.keySet();
//    private final Set<NodeAddress> livingBookingNodesSet = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<NodeAddress> userQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NodeAddress> bookingQueue = new ConcurrentLinkedQueue<>();

    // Network client instace, for starting communications
    private final Client client;
    // Network client instace, for receiving messages
    private final Server server;
    // Heartbeat executor service
    // TODO : Encapsulate this on its own class
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
        System.out.println("Rodando o API Gateway");
        activateHeartbeatWorker();
        server.runServer(this);
        shutdownHeartbeatWorker();
    }


    private final Runnable heartbeatWorker = new Runnable() {
        @Override
        public void run() {
            List<NodeAddress> addresses = new ArrayList<>();
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                System.out.println("Adresses no addressTable : "  + addressTable.keySet());
                System.out.println("addresses no userQueue : "  + userQueue);
                System.out.println("addresses no bookingQueue : "  + bookingQueue);
                for (NodeAddress address : addressTable.keySet()) {
                    NodeStatus nodeStatus = NodeStatus.ALIVE;
                    NodeAddress responseAddress = null;
                    Future<NodeAddress> future = executorService.submit(() -> sendHeartbeat(address, addressTable.get(address)));
                    try {
                        Optional<NodeAddress> retorno  = Optional.ofNullable(future.get(500, TimeUnit.MILLISECONDS));
//                        System.out.println("Retorno do heartbeat : " + retorno);
                        if (retorno.isEmpty()) {
                            nodeStatus = NodeStatus.DEAD;
                        }
                    } catch (TimeoutException e) {
                        future.cancel(true);
                        nodeStatus = NodeStatus.DEAD;
                    } catch (Exception e) {
                        e.printStackTrace();
                        nodeStatus = NodeStatus.DEAD;
                    }
                    updateNodeStatus(address, addressTable.get(address), nodeStatus);
                }
            }
        }
    };

    // TODO : Erros na formatacao da mensagem serao tratadas na camada protocol
    @Override
    public ResponsePayload handle(RequestPayload payload) {
        if (payload.operation().equalsIgnoreCase("ERROR")) {
           return new ResponsePayload(ResponseStatus.ERROR, payload.value(), gatewayAddress);
        }
//        Non functional requests
        if (Objects.equals(payload.operation(), "REGISTER")) {
            NodeAddress senderAddress = NodeAddress.fromString(payload.value());
            return registerNewNode(senderAddress, payload.senderRole());
        }
        // TODO : How to check if a service request is indeed a part of the addressTable

        if (payload.senderRole() != NodeRole.CLIENT && !addressTable.containsKey(payload.destinationAddress())) {
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
        ConcurrentLinkedQueue nodeQueue = switch (nodeRole) {
            case NodeRole.USER -> userQueue;
            case NodeRole.BOOKING -> bookingQueue;
            default -> null;
        };
        if (nodeMap == null) {
            return;
        }
        if (newStatus == NodeStatus.ALIVE) {
            Optional<NodeStatus> currentStatus = Optional.ofNullable(nodeMap.get(nodeAddress));
            if ( currentStatus.isEmpty() || currentStatus.get() == NodeStatus.DEAD || currentStatus.get() == NodeStatus.ALIVE && !nodeQueue.contains(nodeAddress)) {
                nodeQueue.add(nodeAddress);
            }
        } else {
            nodeQueue.remove(nodeAddress); // O(n)
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
        ConcurrentLinkedQueue<NodeAddress> nodeQueue = switch (nodeService) {
            case USER -> userQueue;
            case BOOKING -> bookingQueue;
            case GATEWAY -> null;
            case CLIENT -> null;
        };

        Optional<NodeAddress> address = Optional.ofNullable(nodeQueue.poll());

        if (address.isEmpty()) {
            return Optional.empty();
        }

        if (nodeMap.get(address.get()) == NodeStatus.DEAD) {
           return getLivingNode(nodeService);
        }
        nodeQueue.add(address.get());
        return address;
    }

    private ResponsePayload handleServiceRequest(RequestPayload payload) {
//        System.out.println("Entrou no handleServiceRequest");
        NodeRole service = payload.destinationRole();

        int numOfAttempts = 5;
        Optional<NodeAddress> address = getLivingNode(service);
        if (address.isEmpty()) {
            var erro = new ResponsePayload(ResponseStatus.ERROR, "Internal Error", gatewayAddress);
//            System.out.println("Resposta do serviço : " + erro);
            return new ResponsePayload(ResponseStatus.ERROR, "Internal Error", gatewayAddress);
        }
        if (payload.operation().equalsIgnoreCase("CALCULATE")) {
            System.out.println("# Gateway recebeu calculate");
        }
        NodeAddress chosenNodeAddress = address.get();
        RequestPayload messageToService = new RequestPayload(chosenNodeAddress, service, NodeRole.GATEWAY, payload.operation(), payload.value());
        // Creates a connection to the service node, sends the appropriate request to it and captures the response
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        // If the timeout is exceeded, return an error message to the client
        Future<ResponsePayload> future = executorService.submit(() -> client.sendAndReceive(chosenNodeAddress.ip(), chosenNodeAddress.port(), messageToService));
        try {
            ResponsePayload serviceResponse = future.get(1000, TimeUnit.MILLISECONDS);
            return new ResponsePayload(serviceResponse.status(), serviceResponse.value(), gatewayAddress);
        } catch (Exception e) {
            System.err.println("APIGateway - handleServiceRequest : an exception has occurred : \n" + e.getMessage());
            return new ResponsePayload(ResponseStatus.ERROR, "Internal Error", gatewayAddress);
        }
    }

    // TODO : When the protocol goes to the server, this method wont exist anymore
    private NodeAddress sendHeartbeat(NodeAddress address, NodeRole serviceRole) {
//        System.out.println("enviando heartbeat para : " + address);
        var heartbeat = new RequestPayload(address, NodeRole.GATEWAY, serviceRole,
                "HEARTBEAT", "pending");
        ResponsePayload response = client.sendAndReceive(address.ip(), address.port(), heartbeat);
        if (response == null || response.status() != ResponseStatus.OK) {
            System.out.println("Heartbeat failed for " + address);
            return null;
        }
        return address;
    }

    private ResponsePayload registerNewNode(NodeAddress nodeAddress, NodeRole nodeRole) {
        addressTable.put(nodeAddress, nodeRole);
        updateNodeStatus(nodeAddress, nodeRole, NodeStatus.ALIVE);
        System.out.printf("SUCCESS - Node (%s) registered%n", nodeAddress);
        // TODO : response
        return new ResponsePayload(ResponseStatus.OK, "success", gatewayAddress);
    }

    // Should be in try with resources clause
    public void activateHeartbeatWorker() {
        heartBeatExecutorService.scheduleAtFixedRate(heartbeatWorker, 0, 2000, TimeUnit.MILLISECONDS);
        heartBeatExecutorService.scheduleAtFixedRate(() -> {
            try {
                heartbeatWorker.run();
            } catch (Exception e) { // This catches everything, to prevent the ScheduledExecutor from shutting down
                System.err.println("heartbeatWorker - An exception has occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void shutdownHeartbeatWorker() {
        System.out.println("Shutdown heartbeat worker");
        heartBeatExecutorService.shutdown();
    }
}