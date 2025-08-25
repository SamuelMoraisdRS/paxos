package ufrn.pd.utils;

import ufrn.pd.client.Client;
import ufrn.pd.server.Server;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.*;

public abstract class Paxos {
    // ID of this node
    private int nodeId;

    private int maxKnowPaxosRound; // The most recent paxos round seen by this node, this should be incremented wiht new proposals

    private PaxosPhase paxosPhase;
    // Indicates the node's current generation
    private Generation currentGeneration;
    //    Used to determine if a majority quorum has been reached
    int currentQuorum = 0;

    private Server server;

    //   If a node has a majority quorum, it will unavoidebly lose that status in order for other node to
//    gain majority

    String currentProposal;

    List<Acceptor> acceptors;
    
    private int quorum; // milliseconds

    private int TIMEOUT_PER_ACCEPTOR = 200; // milliseconds

    // This timeout breaks out of the method if a majority of acceptors have crashed
    private final int TOTAL_TIMEOUT = 3000; // milliseconds

    public Paxos(Server server, List<Acceptor> acceptors, int timeout) {
        this.quorum = (int) (acceptors.size()/2) + 1;
        this.server = server;
        this.acceptors = acceptors;
        this.TIMEOUT_PER_ACCEPTOR = timeout;
    }

    public Paxos(Server server, List<Acceptor> acceptors) {
        this.quorum = (int) (acceptors.size()/2) + 1;
        this.server = server;
        this.acceptors = acceptors;
    }

    void runPaxos(int key, int requestId, String operation) {
        // perform new proposal
//       Get the acceptors addresses
//     Broadcast the prepare message to all acceptors
//     wait for the responses, set a timeout
//     once the timeout ends, check promisses received
//     If the value returned by one of the nodes comes from a later generation, resend the promise with the new value
//     if the a quorum is reached, send the accept message to the acceptors. If not, try again with a higher generation
        Generation newGeneration = new Generation(++this.maxKnowPaxosRound, nodeId);
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();) {

            CompletionService completionService = new ExecutorCompletionService<>(executorService);

            for (Acceptor acceptor : acceptors) {
                completionService.submit(() -> prepare(acceptor.getHost(), acceptor.getPort(), newGeneration, operation));
            }

            List<PrepareResponse> responses = new ArrayList<>();

            long startTime = System.currentTimeMillis();
            while (startTime - System.currentTimeMillis() < TOTAL_TIMEOUT && responses.size() < quorum) {
                Future<PrepareResponse> responseFuture = completionService.poll(TIMEOUT_PER_ACCEPTOR, TimeUnit.MILLISECONDS);
                if (responseFuture != null) {
                    // This is non blocking
                  responses.add(responseFuture.get());
                }
                responses.add(completionService.poll().get());
            }
           operation = getMostRecentAcceptedValue(responses);
        } catch (ExecutionException e) {
                System.err.println("Paxos - Proposer : Error retrieving the result of the execution : " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Paxos - Proposer : Executor thread has been interrupted : " + e.getMessage());
        }


    }

    private String getMostRecentAcceptedValue(List<PrepareResponse> responses) {
        responses.stream().reduce()

    }

    // Sends a 'prepare' message to an acceptor
    abstract PrepareResponse prepare(String acceptorHost, int acceptorPort, Generation generationNumber, String value);

    // Sends a 'promise' or 'reject' message to a proposer
    abstract void promise(long generationNumber, long proposalNumber, String proposalValue);

    // Sends an 'accept' message to the other nodes
    abstract void accept(long generationNumber, long proposalNumber, String proposalValue);

    // Sends an 'ok' and updates the respective value or 'reject' message to a proposer
    abstract void handleAccept(long generationNumber, long proposalNumber, String proposalValue);

    // Sends a 'commit' message to the other nodes
    abstract void commit(long generationNumber, long proposalNumber, String proposalValue);


    // Checks if a majority quorum has been reached
    abstract void checkQuorum();
}
