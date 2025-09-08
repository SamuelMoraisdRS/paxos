package ufrn.pd.server;

import ufrn.pd.service.Service;
import ufrn.pd.service.user.dtos.RequestPayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
        try {
            socket.receive(packet);
            // TODO : Receber via codec
            RequestPayload message = protocol.parse(new String(packet.getData()));
            Optional<RequestPayload> responsePayload = Optional.ofNullable(service.handle(message));
            if (responsePayload.isEmpty()) {
                return;
            }
            String reply = protocol.createMessage(responsePayload.get());
            System.out.println("Recebido: " + reply);
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
