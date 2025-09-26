package ufrn.pd.server;

import ufrn.pd.service.Service;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class UDPServerSocket implements ServerSocketAdapter {

    private DatagramSocket socket;
    private int port;
    private int segmentSize;
    private String message;

    public UDPServerSocket(int port, int segmentSize) {
        this.port = port;
        this.segmentSize = 1024;
    }

    @Override
    public void handleConnection(Service service, ApplicationProtocol protocol) {
        DatagramPacket packet = new DatagramPacket(new byte [segmentSize], segmentSize);
//        StringBuilder stringBuilder = new StringBuilder();
        try {
//            int num_linhas = 0;
//            while (num_linhas < 5) {
//                DatagramPacket packet = new DatagramPacket(new byte [segmentSize], segmentSize);
//                socket.receive(packet);
//                stringBuilder.append(new String(packet.getData()));
//
//
//            }
            socket.receive(packet);
            String messageString = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            // TODO : Receber via codec
//            System.out.println("Recebido: " + new String(packet.getData()));
            RequestPayload message = protocol.parseRequest(messageString);
            Optional<ResponsePayload> responsePayload = Optional.ofNullable(service.handle(message));
            if (responsePayload.isEmpty()) {
                return;
            }
            String reply = protocol.createResponse(responsePayload.get());
//            System.out.println("Enviando: \n" + reply);
//            System.out.println("Recebido: " + reply);
            DatagramPacket replyPacket = new DatagramPacket(reply.getBytes(), reply.length(), packet.getAddress(), packet.getPort());
            socket.send(replyPacket);
        } catch (IOException e) {
            System.err.println("UDP Server - Error receiving packet: " + e.getMessage());
        }
        message = new String(packet.getData());
    }

    @Override
    public void open(){
        try {
           socket = new DatagramSocket(port);
        } catch (IOException e) {
            System.err.println("UDP Server - Error opening socket: " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
