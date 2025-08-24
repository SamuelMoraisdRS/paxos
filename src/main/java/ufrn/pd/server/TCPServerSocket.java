package ufrn.pd.server;

import ufrn.pd.service.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServerSocket implements ServerSocketAdapter{

    ServerSocket serverSocket;
    ExecutorService executorService;
    private int THREAD_POOL_SIZE = 100;
    private int connectionPoolSize = 100;
    private int port;

    public TCPServerSocket(int port, int connectionPoolSize) {
        this.port = port;
        this.connectionPoolSize = connectionPoolSize;
    }

    protected void processRequest(Service service, ApplicationProtocol appProtocol, Socket socket) {
        try(PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader socketReader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))){
               String message = socketReader.readLine();
               if (!appProtocol.validateRequest(message)) {
                   throw new IllegalArgumentException("The Request does not match the application protocol");
               }
               String reply = service.handle(message);
                String replyMessage = appProtocol.formatResponse(reply);
               // Send the reply
               socketWriter.println(replyMessage);
            } catch (IOException e) {
                System.err.println("TCP Server - Error acessing socket streams: " + e.getMessage());
            }
    }
    @Override
    public void handleConnection( ApplicationProtocol appProtocol, Service service) {
            try {
                Socket socket = serverSocket.accept();
                this.executorService.execute(() -> processRequest(service, appProtocol, socket));
            } catch (IOException e) {
                System.err.println(" TCP Server - Error stablishing client connection: " + e.getMessage());
            }

    }

    @Override
    public void open() {
        try {
            serverSocket = new ServerSocket(port, connectionPoolSize);
            this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        } catch (IOException e) {
            System.err.println("TCP Server - Error binding server socket : " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        serverSocket.close();
        executorService.shutdownNow();
    }
}
