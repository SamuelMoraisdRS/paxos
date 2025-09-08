package ufrn.pd.service.user.dtos;

import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;

// Record representing a basic request payload
public record RequestPayload(
        // Node's destinationAddress, used to route the request to the appropriate service. For the replies
        // to the client node, this field can contain a dummy address
        NodeAddress destinationAddress,
        // The resource the node is trying to access (HEARTBEAT, USER, REGISTER and BOOKING)
        NodeRole senderRole,
        // The resource the node is trying to access (HEARTBEAT, USER, REGISTER and BOOKING)
        NodeRole destinationRole,
        // The operation that should be performed in the resource
        String operation,
        // The message's funvctional payload
        String value
) {
}
