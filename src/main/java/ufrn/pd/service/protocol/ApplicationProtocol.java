package ufrn.pd.service.protocol;

import ufrn.pd.service.user.dtos.RequestPayload;

import java.io.IOException;

/*
Class that will represent the application's protocol, performing parsing and generating responses
 */
public interface ApplicationProtocol {
    RequestPayload parse(String message) throws IOException;

    String formatResponse(RequestPayload message);

    String createRequest(RequestPayload message);
}
