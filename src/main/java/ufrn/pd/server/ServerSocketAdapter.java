package ufrn.pd.server;
import ufrn.pd.service.Service;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Abstract adapter interface for the server socket objects used in this project
 */
public interface ServerSocketAdapter extends AutoCloseable {

     void handleConnection(ApplicationProtocol appProtocol,  Service service);

     void open();

}
