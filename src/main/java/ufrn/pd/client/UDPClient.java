package ufrn.pd.client;

import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;
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
    public ResponsePayload sendAndReceive(RequestPayload messagePayload) {
        String remoteAddress = messagePayload.destinationAddress().ip();
        int port = messagePayload.destinationAddress().port();
        String reply = null;
        try (DatagramSocket clientSocket = new DatagramSocket();) {
            String message = protocol.createRequest(messagePayload);
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(remoteAddress), port);
            clientSocket.send(packet);
            DatagramPacket replyPacket = new DatagramPacket(new byte[4250], 4250);
            // Unike the TCP Conection, we receive the message as a single packet
            clientSocket.receive(replyPacket);
            String response = new String(replyPacket.getData());
//            System.out.println("Reply recebida no client : " + response);
            return protocol.parseResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
