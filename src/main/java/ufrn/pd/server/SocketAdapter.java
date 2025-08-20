package ufrn.pd.server;
import java.net.InetAddress;

/**
 * Abstract adapter class for the server socket objects used in this project
 */
public abstract class SocketAdapter {
    protected int localPort;
    protected String localAddress;

    protected int remotePort;
    protected String remoteAddress;

    abstract void createSocket(int port, InetAddress host);

    abstract void send(int port, InetAddress host);

    // This should be a blocking operation
    abstract void receive();
}
