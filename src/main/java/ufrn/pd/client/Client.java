package ufrn.pd.client;

import ufrn.pd.service.Service;
import ufrn.pd.service.user.dtos.RequestPayload;
import ufrn.pd.utils.protocol.ApplicationProtocol;

import java.io.IOException;

public interface Client {
    /*
        This method sends exactly one message and receives its response, returning it as a request payload object
     */
    RequestPayload sendAndReceive(String remoteAddress, int port, RequestPayload message);
}
