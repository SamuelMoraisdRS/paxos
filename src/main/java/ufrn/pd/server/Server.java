package ufrn.pd.server

/**
 * This class represents the server application for each of our system's components
 * */ 
public class Server {

    // Adapter object to represent the socket interface used in this project
    SocketAdapter socketAdapter;

    private int port = 3009;

    private String host = "localhost";

    public Server(SocketAdapter socketAdapter) {
        this.socketAdapter = socketAdapter;
    }

    public Server(SocketAdapter socketAdapter, int port, String host) {
        this.socketAdapter = socketAdapter;
        this.port = port;
        this.host = host;
    }

    // Use conf file (or main?)
   public static void main(String[] args) {
        try(Socket socket = socketAdapter.createSocket(port, host)) {

        } catch {

        }
   }

}