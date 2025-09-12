package ufrn.pd.client;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;

public interface Client {
    /*
        This method sends exactly one message and receives its response, returning it as a request payload object
     */
    // TODO : Shoul return ResponsePayload
    ResponsePayload sendAndReceive(String remoteAddress, int port, RequestPayload message);
}
