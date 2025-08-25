package ufrn.pd.utils;

import com.sun.jdi.Value;
import ufrn.pd.client.Client;
import ufrn.pd.server.Server;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public  abstract class Paxos {
   // ID of this node
    int nodeId;
    int maxKnowPaxosRound; // The most recent paxos round seen by this node, this should be incremented wiht new proposals

    PaxosPhase paxosPhase;
    // Indicates the node's current generation
    Generation currentGeneration;
    //    Used to determine if a majority quorum has been reached
    int currentQuorum;
    int numberOfParticipants;
    Server server;
    //   If a node has a majority quorum, it will unavoidebly lose that status in order for other node to
//    gain majority
    Client client;

    Value currentProposal;

    int TIMEOUT;

    void runPaxos(int key, int requestId, String operation) {
        // perform new proposal
//       Get the acceptors addresses
//     Broadcast the prepare message to all acceptors
//     wait for the responses, set a timeout
//     once the timeout ends, check promisses received
//     If the value returned by one of the nodes comes from a later generation, resend the promise with the new value
//     if the a quorum is reached, send the accept message to the acceptors. If not, try again with a higher generation
        Generation newGeneration = new Generation(++this.maxKnowPaxosRound, nodeId);
        List<String> addresses = getAcceptorsAddresses();
        ExecutorService executorService = Executors.newFixedThreadPool(addresses.size());
        for (String acceptorAddress : addresses) {
         // async
         executorService.execute();
         prepare(acceptorAddress, newGeneration, operation);
        }
        Thread.sleep(TIMEOUT);
        sendPrepare(acceptorsAddresses, newGeneration, operation);


    }
    // Sends a 'prepare' message to an acceptor
    abstract void prepare(String acceptorAddress, long generationNumber, long proposalNumber);

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
