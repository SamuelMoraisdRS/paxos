package ufrn.pd.client;

import ufrn.pd.server.ApplicationProtocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UDPClient implements Client {
    private InetAddress remoteAddress;
    private int remotePort;
    private ApplicationProtocol applicationProtocol;
    private int segmentSize = 1024;

    public UDPClient(String remoteAddress, int remotePort, ApplicationProtocol applicationProtocol) {
        try {
            this.remoteAddress = InetAddress.getByName(remoteAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.remotePort = remotePort;
        this.applicationProtocol = applicationProtocol;
    }

    @Override
    public String send(String message) {
        String reply = null;
        try (DatagramSocket clientSocket = new DatagramSocket();) {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), remoteAddress, remotePort);
            if (!applicationProtocol.validateRequest(message)) {
                throw new IllegalArgumentException("The Request does not match the application protocol");
            }
            clientSocket.send(packet);
            DatagramPacket replyPacket = new DatagramPacket(new byte[1024], 1024);
            clientSocket.receive(replyPacket);
            return new String(replyPacket.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reply;
    }
}
