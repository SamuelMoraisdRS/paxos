package ufrn.pd.server;
import ufrn.pd.service.Service;

/**
 * Abstract adapter interface for the server socket objects used in this project
 */
public interface ServerSocketAdapter extends AutoCloseable {

     void handleConnection(Service service);

     void open();

}
