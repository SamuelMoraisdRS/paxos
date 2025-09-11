package ufrn.pd.service.user;

import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.service.user.protocol.ResponseStatus;

public record ResponsePayload(
    ResponseStatus status,
    String value,
    NodeAddress senderAddress
) {
}
