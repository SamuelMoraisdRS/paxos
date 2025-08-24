package ufrn.pd.client;

import ufrn.pd.server.ApplicationProtocol;


import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient implements Client {
    private InetAddress remoteAddress;
    private int remotePort;
    private ApplicationProtocol applicationProtocol;
    private int connectionPoolSize = 1000;

    public TCPClient(String remoteAddress, int remotePort, ApplicationProtocol applicationProtocol, int connectionPoolSize) {
        try {
            this.remoteAddress = InetAddress.getByName(remoteAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.remotePort = remotePort;
        this.applicationProtocol = applicationProtocol;
        this.connectionPoolSize = connectionPoolSize;
    }

    @Override
    public String send(String message) {
        try (Socket clientSocket = new Socket(remoteAddress, remotePort);) {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));) {
                out.println(message);
                return in.readLine();
            } catch (IOException e) {
                System.err.println("TCP Client - Error accessing socket streams: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("TCP Client - Error binding socket: " + e.getMessage());
        }
        return "";
    }
}
