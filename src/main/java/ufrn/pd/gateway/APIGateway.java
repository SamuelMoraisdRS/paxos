package ufrn.pd.gateway;

import ufrn.pd.client.Client;
import ufrn.pd.server.Server;
import ufrn.pd.service.Service;
import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;
import ufrn.pd.utils.protocol.ResponseStatus;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class APIGateway extends Gateway implements Service {

    // Table of addresses to the nodes managed by this gateway
    private final Map<NodeAddress, NodeRole> addressTable = new ConcurrentHashMap<>();

    // ! : High coupling
    private final ConcurrentHashMap<NodeAddress, NodeStatus> BM25ServiceNodes = new ConcurrentHashMap<>();
    private final Map<NodeAddress, NodeStatus> dataStoreNodes = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<NodeAddress> userQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NodeAddress> bookingQueue = new ConcurrentLinkedQueue<>();

    private CopyOnWriteArrayList<NodeAddress> userNodesList = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<NodeAddress> bookingNodesList = new CopyOnWriteArrayList<>();

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
        System.out.println("saiu do run server");
        shutdownHeartbeatWorker();
    }


    private final Runnable heartbeatWorker = new Runnable() {
        @Override
        public void run() {
            try {
                List<NodeAddress> addresses = new ArrayList<>();
                long start = System.nanoTime();

                System.out.println(STR."Nos de BM25Service vivos agora: \{BM25ServiceNodes.entrySet().stream()
                        .filter(e -> e.getValue() == NodeStatus.ALIVE)
                        .map(Map.Entry::getKey)
                        .toList()}");

                System.out.println(STR."Nos de BM25DataStore vivos agora: \{dataStoreNodes.entrySet().stream()
                        .filter(e -> e.getValue() == NodeStatus.ALIVE)
                        .map(Map.Entry::getKey)
                        .toList()}");

                try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                    for (NodeAddress address : addressTable.keySet()) {
                        NodeStatus nodeStatus = NodeStatus.ALIVE;
                        Future<NodeAddress> future = executorService.submit(() -> sendHeartbeat(address, addressTable.get(address)));
                        try {
                            Optional<NodeAddress> retorno = Optional.ofNullable(future.get(500, TimeUnit.MILLISECONDS));
                            if (retorno.isEmpty()) {
                                nodeStatus = NodeStatus.DEAD;
                            }
                        } catch (TimeoutException e) {
                            System.out.println("TimeoutException no heartbeat");
                            future.cancel(true);
                            nodeStatus = NodeStatus.DEAD;
                        } catch (Exception e) {
                            e.printStackTrace();
                            nodeStatus = NodeStatus.DEAD;
                        }

                        updateNodeStatus(address, addressTable.get(address), nodeStatus);
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception in heartbeatWorker.run(): " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("Saiu de tudo");
        }
    };

    @Override
    public ResponsePayload handle(RequestPayload payload) {
        if (payload.operation().equalsIgnoreCase("ERROR")) {
            return new ResponsePayload(ResponseStatus.ERROR, payload.value(), gatewayAddress);
        }
        if (Objects.equals(payload.operation(), "REGISTER")) {

            NodeAddress senderAddress = NodeAddress.fromString(payload.value());
            return registerNewNode(senderAddress, payload.senderRole());
        }
        return handleServiceRequest(payload);
    }

    protected void updateNodeStatus(NodeAddress nodeAddress, NodeRole nodeRole, NodeStatus newStatus) {
        Map<NodeAddress, NodeStatus> nodeMap = switch (nodeRole) {
            case NodeRole.BM25SERVICE -> BM25ServiceNodes;
            case NodeRole.DATASTORE -> dataStoreNodes;
            default -> null;
        };
        if (nodeMap != null) {
            nodeMap.put(nodeAddress, newStatus);
        } else {
            System.out.println(String.format("Node map : %s is null", nodeMap.getClass().getName()));
        }
    }

    // Indexes for load balancing
    private final AtomicInteger userNextIndex = new AtomicInteger(0);
    private final AtomicInteger bookingNextIndex = new AtomicInteger(0);

    private Optional<NodeAddress> getLivingNode(NodeRole nodeService) {
        Map<NodeAddress, NodeStatus> nodeMap = switch (nodeService) {
            case BM25SERVICE -> BM25ServiceNodes;
            case DATASTORE -> dataStoreNodes;
            case GATEWAY -> null;
            case CLIENT -> null;
        };
        var aliveNodes = nodeMap.entrySet().stream()
                .filter(e -> e.getValue() == NodeStatus.ALIVE)
                .map(Map.Entry::getKey)
                .toList();

        if (aliveNodes.isEmpty()) {
            return Optional.empty();
        }

        AtomicInteger currIndex = switch (nodeService) {
            case BM25SERVICE -> userNextIndex;
            case DATASTORE -> bookingNextIndex;
            default -> null;
        };
        int index = Math.abs(currIndex.getAndIncrement() % aliveNodes.size());
        System.out.println("Lista de nos vivos : " + aliveNodes + " index = " + index);
        return Optional.of(aliveNodes.get(index));
    }
    protected ResponsePayload handleServiceRequest(RequestPayload payload) {
        NodeRole service = payload.destinationRole();

        Optional<NodeAddress> address = getLivingNode(service);
        if (address.isEmpty()) {
            return new ResponsePayload(ResponseStatus.ERROR, "Internal Error - No available nodes", gatewayAddress);
        }
        NodeAddress chosenNodeAddress = address.get();
        RequestPayload messageToService = new RequestPayload(chosenNodeAddress, service, NodeRole.GATEWAY, payload.operation(), payload.value());
        // Creates a connection to the service node, sends the appropriate request to it and captures the response
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();) {
            // If the timeout is exceeded, return an error message to the client
            Future<ResponsePayload> future = executorService.submit(() -> client.sendAndReceive(messageToService));
            try {
                ResponsePayload serviceResponse = future.get(3000, TimeUnit.MILLISECONDS);
                if (serviceResponse == null) {
                    return new ResponsePayload(ResponseStatus.ERROR, "Internal Error - novo clausula " , gatewayAddress);
                }
                return new ResponsePayload(serviceResponse.status(), serviceResponse.value(), gatewayAddress);
            } catch (Exception e) {
                System.err.println("APIGateway - handleServiceRequest : an exception has occurred : \n" + e);
                return new ResponsePayload(ResponseStatus.ERROR, "Internal Error - handleServiceRequest : Timeout" + e.getMessage(), gatewayAddress);
            }
        }

    }

    // TODO : When the protocol goes to the server, this method wont exist anymore
    private NodeAddress sendHeartbeat(NodeAddress address, NodeRole serviceRole) {
        var heartbeat = new RequestPayload(address, NodeRole.GATEWAY, serviceRole,
                "HEARTBEAT", "pending");
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        ResponsePayload response = client.sendAndReceive(heartbeat);
        if (response == null || response.status() != ResponseStatus.OK) {
            System.out.println("Heartbeat failed for " + address);
            return null;
        }
        return address;
    }

    protected ResponsePayload registerNewNode(NodeAddress nodeAddress, NodeRole nodeRole) {
        addressTable.put(nodeAddress, nodeRole);
        updateNodeStatus(nodeAddress, nodeRole, NodeStatus.ALIVE);
        System.out.printf("SUCCESS - Node (%s) registered%n", nodeAddress);
        // TODO : response
        return new ResponsePayload(ResponseStatus.OK, "success", gatewayAddress);
    }

    public void activateHeartbeatWorker() {
//        heartBeatExecutorService.scheduleAtFixedRate(heartbeatWorker, 0, 1000, TimeUnit.MILLISECONDS);
        heartBeatExecutorService.scheduleWithFixedDelay(() -> {
            System.out.println("Executa o worker");
            try {
                heartbeatWorker.run();
            } catch (Exception e) { // this catches everything, to prevent the ScheduledExecutor from shutting down
                System.err.println("heartbeatWorker - An exception has occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 400, TimeUnit.MILLISECONDS);
    }

    public void shutdownHeartbeatWorker() {
        System.out.println("Shutdown heartbeat worker");
        heartBeatExecutorService.shutdown();
    }
}