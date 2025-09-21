package ufrn.pd.gateway.protocol;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.utils.protocol.HTTPProtocol;

import java.util.Optional;

public class HTTPGatewayProtocol extends HTTPProtocol {
    @Override
    public boolean validateOperation(String method, String resource) {
        return true;
    }
    @Override
    public Optional<RequestPayload> validateMessage(String msg) {
        return Optional.empty();
    }
}
