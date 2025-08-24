package ufrn.pd.server;

import ufrn.pd.service.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * This class represents the server application for each of our system's components
 * */ 
public class Server {

    // Adapter object to represent the socket interface used in this project
    private final ServerSocketAdapter socket;

    /*
    This object will interpret the message according to the app protocol used here
     */
    private ApplicationProtocol applicationProtocol;

    private Service service;

    public Server(ServerSocketAdapter socket, Service service, ApplicationProtocol applicationProtocol) {
        this.socket = socket;
        this.service = service;
        this.applicationProtocol = applicationProtocol;
    }

//    Meant for UDP servers
    public void runServer() {
        try (socket) {
            socket.open();
            while (true) {
                socket.handleConnection(applicationProtocol, service);
            }
        } catch (Exception e) {
           e.printStackTrace();
        }
    }
}