package ufrn.pd.client;

import ufrn.pd.service.Service;


import java.io.*;
import java.net.Socket;

public class TCPClient implements Client {
    private int connectionPoolSize = 1000;

    public TCPClient() {
    }

    public TCPClient(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    @Override
    public  String sendAndReceive(String remoteAddress, int remotePort, String message, Service service) {
        try (Socket clientSocket = new Socket(remoteAddress, remotePort);) {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));) {
                out.println(message);
                System.out.println(service.handle(message)) ;
                return service.handle(message);
            } catch (IOException e) {
                System.err.println("TCP Client - Error accessing socket streams: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("TCP Client - Error binding socket: " + e.getMessage());
        }
        return "";
    }

    @Override
    public String sendAndReceive(String remoteAddress, int port, String message) {
        return "";
    }
}
