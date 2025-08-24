package ufrn.pd.client;

import ufrn.pd.server.ApplicationProtocol;

import java.net.InetAddress;

public interface Client {
    String send(String message);
}
