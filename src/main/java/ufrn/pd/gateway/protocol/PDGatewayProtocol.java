package ufrn.pd.gateway.protocol;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.utils.protocol.PDProtocol;

import java.util.List;
import java.util.Optional;

public class PDGatewayProtocol extends PDProtocol {
    @Override
    public Optional<RequestPayload> validateMessage(String msg) {
        // TODO : Implement the validation logic
//        if (msg.size() < 6) {
//            return Optional.of(RequestPayload.createErrorMessage(msg));
//        }
//
//        if (msg[])
//
        return Optional.empty();
    }
}
