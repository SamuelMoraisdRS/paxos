package ufrn.pd.utils;

import java.util.Optional;

/*
 This class represents the Acceptor role of Paxos design pattern. Every class that takes part in the paxos election
 process should have an Acceptor and a Proposer instance.

 The Acceptor is responsible for receiving the prepare and accept messages from the proposer and sending the promise
 or reject message to the proposer.
  */
public class Acceptor {
    // This is the highest possible generation, the one assimilated when a prepare message is received and accepted
    private  Optional<Generation> promissedGeneration = Optional.empty();
    // This is the generation of the previous accepted value on the algorithm
    private  Optional<Generation> acceptedGeneration = Optional.empty();
    // The previously accepted value
    private  Optional<String> acceptedValue= Optional.empty();
    // The value that has been chosen
    private  Optional<String> commitedValue= Optional.empty();

    public Acceptor(){}


}
