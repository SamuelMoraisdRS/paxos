package ufrn.pd.utils.protocol;

import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;

import java.util.Optional;

/*
Class that will represent the application's protocol, performing parsing and generating responses
 */
// TODO : Turn this into an abstract class and create the validation and error messages hooks
public interface ApplicationProtocol {
    Optional<RequestPayload> validateMessage(String msg);
    RequestPayload parseRequest(String message);
    ResponsePayload parseResponse(String message);
    String createRequest(RequestPayload message);
    String createResponse(ResponsePayload message);
}
