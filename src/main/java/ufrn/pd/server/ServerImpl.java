package ufrn.pd.server;

import ufrn.pd.service.Service;
import ufrn.pd.utils.protocol.ApplicationProtocol;


/**
 * This class represents the server application for each of our system's components
 * */ 
public class ServerImpl implements Server {

    // Adapter object to represent the socket interface used in this project
    private final ServerSocketAdapter socket;

    /*
    This object will interpret the message according to the app protocol used here
     */

    private Service service;
    private ApplicationProtocol protocol;

    public ServerImpl(ServerSocketAdapter socket, ApplicationProtocol protocol) {
        this.socket = socket;
        this.protocol = protocol;
    }

    public ServerImpl(ServerSocketAdapter socket) {
        this.socket = socket;
    }

    @Override
    public void runServer(Service service) {
        try (socket) {
            socket.open();
            while (true) {
                socket.handleConnection(service, protocol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}