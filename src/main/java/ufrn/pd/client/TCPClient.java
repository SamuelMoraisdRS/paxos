package ufrn.pd.client;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;


import java.io.*;
import java.net.Socket;

public class TCPClient implements Client {
    private int connectionPoolSize = 1000;
    private Socket clientSocket;
    private BufferedReader socketReader;
    private PrintWriter socketWriter;
    private final ApplicationProtocol protocol;

    public TCPClient(ApplicationProtocol protocol) {
        this.protocol = protocol;
    }

    public TCPClient(int connectionPoolSize, ApplicationProtocol protocol) {
        this.connectionPoolSize = connectionPoolSize;
        this.protocol = protocol;
    }


    // TODO : Examine the systems fault tolerance
    @Override
    public ResponsePayload sendAndReceive(String remoteAddress, int remotePort, RequestPayload messagePayload) {
        try (Socket clientSocket = new Socket(remoteAddress, remotePort);) {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                 BufferedReader in = new BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));) {
                String message = protocol.createRequest(messagePayload);
                out.print(message);
                out.flush();
                // TODO : Encapsulate this on a codec class
                String rawResponse = Codec.decodeHttpMessage(in);
                return protocol.parseResponse(rawResponse);
            } catch (IOException e) {
                System.err.println("TCP Client - Error accessing socket streams: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("TCP Client - Error binding socket: " + e.getMessage());
        }
        return null;
    }
}
