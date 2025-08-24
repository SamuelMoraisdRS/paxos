package ufrn.pd.server;

import ufrn.pd.service.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServerSocket implements ServerSocketAdapter {
    private DatagramSocket socket;
    private int port;
    private int segmentSize;
    private String message;

    public UDPServerSocket(int port, int segmentSize) {
        this.port = port;
        this.segmentSize = 1024;
    }

    // Blocking operation
    @Override
    public void handleConnection(ApplicationProtocol appProtocol, Service service) {
        DatagramPacket packet = new DatagramPacket(new byte [segmentSize], segmentSize);
        try {
            socket.receive(packet);
            String reply = service.handle(new String(packet.getData()));
            System.out.println("Recebido: " + reply);
            if (!appProtocol.validateRequest(reply)) {
                throw new IllegalArgumentException("The Request does not match the application protocol");
            }
            String replyMessage = appProtocol.formatResponse(reply);
            DatagramPacket replyPacket = new DatagramPacket(replyMessage.getBytes(), replyMessage.length(), packet.getAddress(), packet.getPort());
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
