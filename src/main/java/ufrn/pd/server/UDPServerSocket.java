package ufrn.pd.server;

import ufrn.pd.service.Service;
import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.*;

public class UDPServerSocket implements ServerSocketAdapter {

    private DatagramSocket socket;
    private int port;
    private int segmentSize;
    private String message;
    private ExecutorService executorService;

    public UDPServerSocket(int port, int segmentSize) {
        this.port = port;
        this.segmentSize = segmentSize;
    }

    public void processRequest(Service service, ApplicationProtocol protocol, DatagramSocket socket) {

        DatagramPacket packet = new DatagramPacket(new byte[segmentSize], segmentSize);
        try {
            socket.receive(packet);
            String messageString = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            // TODO : Receber via codec
            RequestPayload message = protocol.parseRequest(messageString);
            Optional<ResponsePayload> responsePayload = Optional.ofNullable(service.handle(message));
            if (responsePayload.isEmpty()) {
                return;
            }
            String reply = protocol.createResponse(responsePayload.get());
//            System.out.println("Bytes no reply " + reply.getBytes().length);
//            System.out.println("Reply recebida no server " + reply);
            DatagramPacket replyPacket = new DatagramPacket(reply.getBytes(), reply.getBytes().length, packet.getAddress(), packet.getPort());
            socket.send(replyPacket);
        } catch (IOException e) {
            System.err.println("UDP Server - Error receiving packet: " + e.getMessage());
        }
//        message = new String(packet.getData());
    }

    @Override
    public void handleConnection(Service service, ApplicationProtocol protocol) {
        try {
//            DatagramSocket socket = new DatagramSocket(port);
//            processRequest(service, protocol, socket);
            this.executorService.execute(() -> processRequest(service, protocol, socket));

        } catch (Exception e) {
            System.err.println("address" + this.port);
            System.err.println(" UDP Server - Error stablishing client connection: " + e.getMessage());
        }

    }

    @Override
    public void open() {
        try {
//            executorService = Executors.newCachedThreadPool();
            // Para impedir o thread pool de explodir, usamos uma queue bloqueante pra aceita as requisicoes
            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(550);
            this.executorService = new ThreadPoolExecutor(
                    0, 450, 10L, TimeUnit.SECONDS,
                    queue,
                    (r, executor) -> {
                        try {
                            executor.getQueue().put(r); // bloqueia se a queue tiver cheia
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
            );
            socket = new DatagramSocket(port);
        } catch (IOException e) {
            System.err.println("UDP Server - Error opening socket: " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        socket.close();
        executorService.shutdownNow();
    }
}
