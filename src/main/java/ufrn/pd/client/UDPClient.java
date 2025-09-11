package ufrn.pd.client;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient implements Client {
    private int segmentSize = 1024;
    private final ApplicationProtocol protocol;

    public UDPClient(ApplicationProtocol protocol) {
        this.protocol = protocol;
    }

    public UDPClient(int segmentSize, ApplicationProtocol protocol) {
        this.segmentSize = segmentSize;
        this.protocol = protocol;
    }

    @Override
    public ResponsePayload sendAndReceive(String remoteAddress, int port, RequestPayload messagePayload) {
        String reply = null;
        try (DatagramSocket clientSocket = new DatagramSocket();) {
            String message = protocol.createRequest(messagePayload);
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(remoteAddress), port);
            clientSocket.send(packet);
            DatagramPacket replyPacket = new DatagramPacket(new byte[1024], 1024);
            // Unike the TCP Conection, we receive the message as a single packet
            clientSocket.receive(replyPacket);
            String response = new String(replyPacket.getData());
            return protocol.parseResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
