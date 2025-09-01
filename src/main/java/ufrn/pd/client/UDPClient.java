package ufrn.pd.client;

import ufrn.pd.service.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient implements Client {
    private int segmentSize = 1024;

    public UDPClient() {
    }

    public UDPClient( int segmentSize) {
        this.segmentSize = segmentSize;
    }

    @Override
    public String sendAndReceive(String remoteAddress, int remotePort, String message, Service service) {
        String reply = null;
        try (DatagramSocket clientSocket = new DatagramSocket();) {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(remoteAddress), remotePort);
            clientSocket.send(packet);
            DatagramPacket replyPacket = new DatagramPacket(new byte[1024], 1024);
            clientSocket.receive(replyPacket);
            return new String(replyPacket.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String sendAndReceive(String remoteAddress, int port, String message) {
        String reply = null;
        try (DatagramSocket clientSocket = new DatagramSocket();) {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(remoteAddress), port);
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
