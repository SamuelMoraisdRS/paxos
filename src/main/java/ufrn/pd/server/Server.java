package ufrn.pd.server;

import ufrn.pd.service.Service;


/**
 * This class represents the server application for each of our system's components
 * */ 
public class Server {

    // Adapter object to represent the socket interface used in this project
    private final ServerSocketAdapter socket;

    /*
    This object will interpret the message according to the app protocol used here
     */

    private final Service service;

    public Server(ServerSocketAdapter socket, Service service) {
        this.socket = socket;
        this.service = service;
    }

    public void runServer() {
        try (socket) {
            socket.open();
            while (true) {
                socket.handleConnection(service);
            }
        } catch (Exception e) {
           e.printStackTrace();
        }
    }
    public void runServer(Service service) {
        try (socket) {
            socket.open();
            while (true) {
                socket.handleConnection(service);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}