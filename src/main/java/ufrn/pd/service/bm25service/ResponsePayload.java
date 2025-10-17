package ufrn.pd.service.bm25service;

import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.utils.protocol.ResponseStatus;

public record ResponsePayload(
    ResponseStatus status,
    String value,
    NodeAddress senderAddress
) {
}
