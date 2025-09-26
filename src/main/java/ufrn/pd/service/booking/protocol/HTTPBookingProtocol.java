package ufrn.pd.service.booking.protocol;

import ufrn.pd.utils.protocol.HTTPProtocol;

public class HTTPBookingProtocol extends HTTPProtocol {
    @Override
    public boolean validateOperation(String method, String resource) {
        return true;
    }
}
