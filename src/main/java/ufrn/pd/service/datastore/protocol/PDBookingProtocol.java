package ufrn.pd.service.datastore.protocol;

import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.utils.protocol.PDProtocol;

import java.util.Optional;

public class PDBookingProtocol extends PDProtocol {
    @Override
    public Optional<RequestPayload> validateMessage(String msg) {
        return Optional.empty();
    }
}