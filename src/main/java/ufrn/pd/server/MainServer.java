package ufrn.pd.server;

import ufrn.pd.service.PaxosService;

public class MainServer {
    public static void main(String[] args) {
        Server server = new Server(new TCPServerSocket(3009, 1000), new PaxosService(),
                new PaxosProtocol());
        server.runServer();
    }
}
