package ufrn.pd.server

/**
 * This class represents the server application for each of our system's components
 * */ 
public class Server {

    // Adapter object to represent the socket interface used in this project
    SocketAdapter socketAdapter;

    private int port = 3009;

    private String host = "localhost";

    private int segmentSize = 1024;

    /*
    This object will interpret the message according to the app protocol used here
     */
    private MessageValidator messageValidator;

    public Server(SocketAdapter socketAdapter) {
        this.socketAdapter = socketAdapter;
    }

    public Server(SocketAdapter socketAdapter, int port, String host) {
        this.socketAdapter = socketAdapter;
        this.port = port;
        this.host = host;
    }

//    TODO: Multithreading
    public void runServer() {
        socketAdapter.open();
        while (true) {
            byte[] clientSegment = new byte[segmentSize];
            socketAdapter.receive();
            messageValidator.validateMessage()

        }
    }
}