package ufrn.pd.server;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSocket extends SocketAdapter {
    private DatagramSocket socket;


    public UDPSocket(int localPort, String localAddress, int remotePort, String remoteAddress) {
        this.localPort = localPort;
        this.localAddress = localAddress;
        this.remotePort = remotePort;
        this.remoteAddress = remoteAddress;
    }

    @Override
    void createSocket(int port, InetAddress host) {


    }

    @Override
    void send(int port, InetAddress host) {

    }

    // Should be a blocking operation
    @Override
    void receive() {

    }

    @Override
    void open() {

    }

    @Override
    String getMessage() {
        return "";
    }

}
