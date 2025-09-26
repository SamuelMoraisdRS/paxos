package ufrn.pd.gateway;

import ufrn.pd.client.Client;
import ufrn.pd.server.Server;

public record NetworkStack(Client client, Server server) {
}
