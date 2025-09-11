package ufrn.pd.service.user.protocol;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.utils.protocol.PDProtocol;

import java.util.List;
import java.util.Optional;

public class PDUserProtocol extends PDProtocol {

    static enum Operations {
        HEARTBEAT, ERROR, END
    }

    @Override
    public Optional<RequestPayload> validateMessage(String message)  {
        // TODO : Implement validation logic
//        if (message.size() < 6) {
//            return false;
//        }
//
//        try  {
//           Operations.valueOf(message.get(0));
//        } catch (IllegalArgumentException e) {
//            return Optional;
//        }

        return Optional.empty();
    }
}
