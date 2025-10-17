package ufrn.pd.gateway;

import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;

public abstract class Gateway {
    protected abstract ResponsePayload handleServiceRequest(RequestPayload requestPayload);
    protected abstract ResponsePayload registerNewNode(NodeAddress address, NodeRole nodeRole);
    protected abstract void updateNodeStatus(NodeAddress nodeAddress, NodeRole nodeRole,NodeStatus newStatus);
}
