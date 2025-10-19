package ufrn.pd.service.bm25service;

import ufrn.pd.client.Client;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.server.Server;
import ufrn.pd.service.Service;
import ufrn.pd.service.ServiceNode;
import ufrn.pd.utils.protocol.ResponseStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class BM25Service implements Service, ServiceNode {
    private final NodeAddress gatewayAddress;
    private final NodeAddress thisNodeAddress;
    private final Client client;
    private final Server server;

    public BM25Service(NodeAddress gatewayAddress, NodeAddress thisNodeAddress, Client client, Server server) {
        this.gatewayAddress = gatewayAddress;
        this.thisNodeAddress = thisNodeAddress;
        this.client = client;
        this.server = server;
    }

    // When the protocol layer is attached to the server, this method will
    // receive request payloads and multiplex
    // them to the appropriate handle function
    @Override
    public ResponsePayload handle(RequestPayload request) {
//        System.out.println("Operation" + request.operation());
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
        if (request.operation().equalsIgnoreCase("SEARCH")) {
            return handleSearch(request.value());
        }

        return new ResponsePayload(ResponseStatus.ERROR,
                "Unknown operation: " + request.operation(), thisNodeAddress);
    }

    private ResponsePayload handleCreate(String userValue) {
        String[] values = userValue.split(":");
        // TODO : The deserialization should be done on the protocol layer
        String userName = values[0];
        String score = values[1];
        ResponsePayload responsePayload = new ResponsePayload(ResponseStatus.OK,
                String.format("User Created - Name : %s , Score : %s", userName, score), thisNodeAddress);
//        System.out.println(responsePayload);
        return responsePayload;
    }



    public double score(String[] doc, String term, BM25Params params) {
        int freq = 0;
        for (String word : doc) {
            if (word.equals(term))
                freq++;
        }

        if (freq == 0) {
            return 0;
        }

        int N = params.corpus().size();
        int df_t = params.documentFrequency().getOrDefault(term, 0);
        double idf = Math.log(1 + (N - df_t + 0.5) / (df_t + 0.5));
        double docLength = doc.length;
        double k1 = params.k1();
        double b = params.b();
        double avgDocLength = params.avgDocLength();
        double norm = freq * (k1 + 1) / (freq + k1 * (1 - b + b * (docLength / avgDocLength)));

        return idf * norm;
    }

    // TODO Move
    private Map<String, Double> search(String query, BM25Params params) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        int docId = 0;
        Map<String, Double> scores = new HashMap<>();
        for (String document : params.corpus()) {
            double totalScore = 0.;
            String[] doc = document.toLowerCase().split("\\s+");
            for (String queryTerm : queryTerms) {
                totalScore += score(doc, queryTerm, params);
            }
            docId++;
            scores.put(document, totalScore);
        }
        return scores;
    }

    private ResponsePayload handleSearch(String calculateScoreValue) {

        String[] values = calculateScoreValue.split("\\|\\|\\|");
        String query = values[0];
        int numberOfResults = Integer.parseInt(values[1]);
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        RequestPayload requestParametersMessage = new RequestPayload(gatewayAddress, NodeRole.BM25SERVICE, NodeRole.DATASTORE,
                "PARAMETERS", "");
        Future<ResponsePayload> response = executorService.submit(() -> client.sendAndReceive(requestParametersMessage));
        try {
            BM25Params params = BM25ParamsMapper.deserializeBM25Params(response.get(4000, TimeUnit.MILLISECONDS).value());
            Map<String, Double> searchScores = search(query, params);
            // n top results for the query
            List<Map.Entry<String, Double>> topResults = searchScores.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(numberOfResults)
                    .toList();
            StringBuilder searchResponsePayload = new StringBuilder();
            int i = 1;
            for (Map.Entry<String, Double> entry : topResults) {
               searchResponsePayload.append(String.format("%d : %s - %.2f", i, entry.getKey(),
                       entry.getValue()));
               i++;
            }
            return new ResponsePayload(ResponseStatus.OK, searchResponsePayload.toString(), thisNodeAddress);
        } catch (InterruptedException e) {
            System.err.println("BM25Service - Interrupted");
            return new ResponsePayload(ResponseStatus.ERROR, "Error performing search - Interruption", thisNodeAddress);
        } catch (ExecutionException e) {
            System.err.println("BM25Service - Execution exception");
            return new ResponsePayload(ResponseStatus.ERROR, "Error performing search - Execution", thisNodeAddress);
        } catch (TimeoutException e) {
            System.err.println("BM25Service - timeout exception");
            return new ResponsePayload(ResponseStatus.ERROR, "Error performing search - Timeout", thisNodeAddress);
        }
    }

    private ResponsePayload handleHeartbeat(RequestPayload request) {
        ResponsePayload heartbeatResponse = new ResponsePayload(ResponseStatus.OK,
                "Heartbeat received", gatewayAddress);
        // System.out.println("Retornando HeartBeat");
        return heartbeatResponse;

    }

    public void run() {
        if (this.raise()) {
            server.runServer(this);
        } else {
            System.err.println("Registration failed");
        }
    }

    @Override
    public boolean raise() {
        RequestPayload registerRequestPayload = new RequestPayload(gatewayAddress, NodeRole.BM25SERVICE, NodeRole.GATEWAY,
                // TODO : Isso deveria estar na camada de protocolo
                "REGISTER", thisNodeAddress.toString());
        ResponsePayload response = client.sendAndReceive(registerRequestPayload);
        System.out.println("REGISTRADO");
        return true;
    }
}
