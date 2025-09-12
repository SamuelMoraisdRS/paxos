package ufrn.pd.server;
import ufrn.pd.service.Service;
import ufrn.pd.utils.protocol.ApplicationProtocol;

/**
 * Abstract adapter interface for the server socket objects used in this project
 */
public interface ServerSocketAdapter extends AutoCloseable {

     void handleConnection(Service service, ApplicationProtocol protocol);

     void open();

}
