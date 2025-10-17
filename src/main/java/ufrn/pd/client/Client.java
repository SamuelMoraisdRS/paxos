package ufrn.pd.client;

import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;

public interface Client {
    /*
        This method sends exactly one message and receives its response, returning it as a request payload object
     */
    // TODO : Shoul return ResponsePayload
    ResponsePayload sendAndReceive(RequestPayload message);
}
