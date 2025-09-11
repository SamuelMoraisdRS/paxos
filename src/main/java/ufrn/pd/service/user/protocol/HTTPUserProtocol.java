package ufrn.pd.service.user.protocol;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.utils.protocol.HTTPProtocol;

import java.util.List;
import java.util.Optional;

public class HTTPUserProtocol extends HTTPProtocol {

    @Override
    public Optional<RequestPayload> validateMessage(String msg) {
        return Optional.empty();
    }
}
