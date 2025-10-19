package ufrn.pd.service.datastore;

import ufrn.pd.client.Client;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.server.Server;
import ufrn.pd.service.Service;
import ufrn.pd.service.ServiceNode;
import ufrn.pd.service.bm25service.BM25Params;
import ufrn.pd.service.bm25service.BM25ParamsMapper;
import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;
//import ufrn.pd.utils.Acceptor;
//import ufrn.pd.utils.Learner;
//import ufrn.pd.utils.Paxos;
import ufrn.pd.utils.protocol.ResponseStatus;

import java.util.*;

// TODO : Bring the general structure up to the ServiceNode abstract class
public class BM25DataStore implements Service, ServiceNode {

    private final Server server;
    private final Client client;
    private final NodeAddress gatewayAddress;
    private final NodeAddress thisNodeAddress;
    private Map<String, Integer> df = new HashMap<>();
    private double avgDocLength = 0;
    private volatile double k1 = 1.5;
    private volatile double b = 0.75;

    private final List<String> corpus = Arrays.asList(
            "Java's high-performance JVM is often the backbone for distributed AI inference services.",
            "Building resilient microservices in Java is foundational for modern distributed AI applications.",
            "The Java ecosystem offers robust libraries for inter-service communication in distributed systems.",
            "Leveraging Java concurrency to handle asynchronous data streams in distributed machine learning pipelines.",
            "Distributed systems necessitate efficient Java serialization for moving AI model parameters across nodes.",
            "AI-driven resource allocation within a distributed Java application cluster.",
            "Implementing consensus algorithms in Java to ensure data consistency across a distributed AI knowledge base.",
            "Java frameworks like Spring Boot simplify the deployment of AI models as distributed RESTful services.",
            "The challenge of distributed tracing in a Java microservices architecture hosting various AI components.",
            "Scaling Java-based message queues to support high-throughput communication between distributed AI agents.",
            "AI algorithms are increasingly used to optimize network topology in distributed Java environments.",
            "Java's platform independence aids in deploying distributed AI solutions across heterogeneous cloud infrastructure.",
            "Monitoring the health and performance of a distributed system using Java telemetry and AI anomaly detection.",
            "Integrating Java big data tools like Apache Spark for distributed processing of AI training datasets.",
            "Distributed ledger technology, often implemented in Java, can provide an immutable audit trail for AI decisions.",
            "The synergy between Java's strong typing and the complexity of distributed AI data structures.",
            "Orchestrating containerized Java services that form the core of a distributed deep learning platform.",
            "Designing fault-tolerant Java components for a distributed system where AI models must remain constantly available.",
            "Using Java to develop the API gateway that routes requests to various distributed AI prediction services.",
            "The future of enterprise AI depends on reliable, distributed systems built on languages like Java."
    );

    private BM25Params bm25Params;

    private void loadDocuments() {
        int totalLength = 0;

//        System.out.println("Leitura Completa.");

        for (String document : corpus) {
            String[] terms = document.split("\\s+");

            totalLength += terms.length;

            // Atualizar document frequency apenas com os termos únicos
            Set<String> uniqueTerms = new HashSet<>();
            for (String term : terms) {
                term = term.intern(); // Reduz memória duplicada
                uniqueTerms.add(term);
            }
            for (String term : uniqueTerms) {
                df.put(term, df.getOrDefault(term, 0) + 1);
            }
            terms = null;
            uniqueTerms = null;
        }
        if (!corpus.isEmpty()) {
            avgDocLength = (double) totalLength / corpus.size();
        }

//        System.out.println("Processamento do corpus finalizada.");
    }

    public BM25DataStore(Server server, Client client, NodeAddress gatewayAddress, NodeAddress thsiNodeAddress) {
        this.server = server;
        this.client = client;
        this.gatewayAddress = gatewayAddress;
        this.thisNodeAddress = thsiNodeAddress;
        loadDocuments();
        this.bm25Params = new BM25Params(corpus, df, k1, b, avgDocLength);
    }


    @Override
    public ResponsePayload handle(RequestPayload request) {
        if (request.operation().equalsIgnoreCase("ERROR")) {
            return new ResponsePayload(ResponseStatus.ERROR, request.value(), thisNodeAddress);
        }
        // TODO : Wrap the operations into enums for each service + gateway
        if (request.operation().equalsIgnoreCase("HEARTBEAT")) {
//            System.out.println("Recebeu um HEARTBEAT");
            return handleHeartbeat(request);
        }
        if (request.operation().equalsIgnoreCase("PARAMETERS")) {
//            System.out.println("Recebeu um PARAMETERS");
            return handleParameters(request.value());
        }
//        System.out.printf("Node : %s - Mensagem recebida:%n%s", thisNodeAddress, request);
        return new ResponsePayload(ResponseStatus.ERROR,
                "Unknown operation: " + request.operation(), thisNodeAddress);
    }


    private ResponsePayload handleParameters(String value) {
        return new ResponsePayload(ResponseStatus.OK,
                BM25ParamsMapper.serializeBM25Params(this.bm25Params), thisNodeAddress);
    }

    private ResponsePayload handleHeartbeat(RequestPayload request) {
        return new ResponsePayload(ResponseStatus.OK,
                "Heartbeat received", gatewayAddress);
    }

    @Override
    public boolean raise() {
        int numOfAttempts = 5;
        RequestPayload registerRequestPayload = new RequestPayload(gatewayAddress, NodeRole.DATASTORE, NodeRole.GATEWAY,
                // TODO : Move to  protocol layer
                "REGISTER", thisNodeAddress.toString());
        ResponsePayload response = client.sendAndReceive(registerRequestPayload);
        System.out.println("REGISTRADO");
        return true;
    }


    public void run() {
        if (this.raise()) {
            server.runServer(this);
        } else {
            System.err.println("Nao conseguiu registrar");
        }

    }
}
