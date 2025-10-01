package ufrn.pd.server;

import ufrn.pd.service.Service;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServerSocket implements ServerSocketAdapter {

    ServerSocket serverSocket;
    ExecutorService executorService;
    private int THREAD_POOL_SIZE = 100;
    private int connectionPoolSize = 100;
    private int port;

    public TCPServerSocket(int port, int connectionPoolSize) {
        this.port = port;
        this.connectionPoolSize = connectionPoolSize;
    }

    private String readHttpMessage(BufferedReader socketReader) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        String line;
        int contentLength = 0;
        // Lê os headers até a linha vazia
        while ((line = socketReader.readLine()) != null && !line.isEmpty()) {
            headerBuilder.append(line).append("\n");
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":", 2)[1].trim());
            }
        }
        // Linha vazia que separa header e body
        headerBuilder.append("\n");
        // Lê o corpo se existir
        char[] bodyChars = new char[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {
            int read = socketReader.read(bodyChars, totalRead, contentLength - totalRead);
            if (read == -1) break; // fim do stream
            totalRead += read;
        }
        return headerBuilder.toString() + new String(bodyChars, 0, totalRead);
    }

    protected void processRequest(Service service, Socket socket, ApplicationProtocol protocol) {
        try (PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader socketReader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {
            // TODO : Encapsulate on a codec class
            String messageString = readHttpMessage(socketReader);
            if (messageString.contains("calculate")){
                System.out.println("# Servidor recebeu calculate");
            }
//            System.out.println("Recebido: " + messageString);
            RequestPayload request = protocol.parseRequest(messageString);
            Optional<ResponsePayload> responsePayload = Optional.ofNullable(service.handle(request));
            if (responsePayload.isEmpty()) {
                return;
            }
            String response = protocol.createResponse(responsePayload.get());
//            System.out.println("Resposta enviada\n" + response);
            socketWriter.println(response);

        } catch (IOException e) {
            System.err.println("TCP Server - Error acessing socket streams: " + e.getMessage());
        }
    }

    @Override
    public void handleConnection(Service service, ApplicationProtocol protocol) {
        try {
            Socket socket = serverSocket.accept();
            this.executorService.execute(() -> processRequest(service, socket, protocol));
        } catch (IOException e) {
            System.err.println(" TCP Server - Error stablishing client connection: " + e.getMessage());
        }

    }

    @Override
    public void open() {
        try {
            serverSocket = new ServerSocket(port);
            this.executorService = Executors.newCachedThreadPool();
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
