package ufrn.pd.client;

import ufrn.pd.service.Service;

public interface Client {
    String sendAndReceive(String remoteAddress, int port, String message, Service service);
    String sendAndReceive(String remoteAddress, int port, String message);
}
