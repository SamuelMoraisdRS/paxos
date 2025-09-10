package ufrn.pd.utils.protocol;

import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.user.dtos.RequestPayload;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/*
Class that will represent the application's protocol, performing parsing and generating responses
 */
// TODO : Turn this into an abstract class and create the validation and error messages hooks
public interface ApplicationProtocol {
    Optional<RequestPayload> validateMessage(List<String> msg);
    RequestPayload parse(String message);
    String createMessage(RequestPayload message);
}
