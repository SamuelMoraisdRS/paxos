package ufrn.pd.service.booking.protocol;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.utils.protocol.PDProtocol;

import java.util.Optional;

public class PDBookingProtocol extends PDProtocol {
    @Override
    public Optional<RequestPayload> validateMessage(String msg) {
        return Optional.empty();
    }
}