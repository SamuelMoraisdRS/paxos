package ufrn.pd.gateway;

import projetogrpc.NodeAddressGRPC;
import projetogrpc.NodeRoleGRPC;
import projetogrpc.ResponseStatus;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;

public class GRPCMapper {

    public static NodeAddressGRPC toNodeAddressGRPC(NodeAddress nodeAddress) {
        return NodeAddressGRPC.newBuilder().setIp(nodeAddress.ip()).setPort(nodeAddress.port()).build();
    }

    public static NodeAddress toNodeAddress(NodeAddressGRPC nodeAddress) {
        return new NodeAddress(nodeAddress.getIp(), nodeAddress.getPort());
    }

    public static RequestPayload toRequestPayload(projetogrpc.Request request) {
        NodeAddress requestDestinationAddress = toNodeAddress(request.getAddress());
        NodeRole requestSenderRole = NodeRole.valueOf(request.getSenderRole().toString());
        NodeRole requestDestinationRole = NodeRole.valueOf(request.getDestinationRole().toString());
        return new RequestPayload(requestDestinationAddress, requestSenderRole,
                requestDestinationRole, request.getOperation(), request.getValue());
    }

    public static projetogrpc.Request toRequestMessage (RequestPayload requestPayload) {
        NodeAddressGRPC requestDestinationAddress = toNodeAddressGRPC(requestPayload.destinationAddress());
        NodeRoleGRPC senderRole = NodeRoleGRPC.valueOf(requestPayload.senderRole().toString());
        NodeRoleGRPC destinationRole = NodeRoleGRPC.valueOf(requestPayload.destinationRole().toString());
        return projetogrpc.Request.newBuilder().setAddress(requestDestinationAddress).setSenderRole(senderRole).
                setDestinationRole(destinationRole).setOperation(requestPayload.operation()).
                setValue(requestPayload.value()).build();

    }

    public static ResponsePayload toResponsePayload(projetogrpc.Response responseMessage) {
        ufrn.pd.service.user.protocol.ResponseStatus responseStatus = ufrn.pd.service.user.protocol.ResponseStatus.valueOf(responseMessage.getStatus().toString());
        NodeAddress senderAddress = new NodeAddress(responseMessage.getSenderAddress().getIp(), responseMessage.getSenderAddress().getPort());
        return new ResponsePayload(responseStatus, responseMessage.getValue(), senderAddress);
    }

    public static projetogrpc.Response toResponseMessage(ResponsePayload responsePayload) {
        NodeAddressGRPC responseSenderAddress = NodeAddressGRPC.newBuilder().
                setIp(responsePayload.senderAddress().ip()).
                setPort(responsePayload.senderAddress().port()).build();

        ResponseStatus responseStatus = ResponseStatus.valueOf(responsePayload.status().toString());

        return projetogrpc.Response.newBuilder().setSenderAddress(responseSenderAddress).
                setStatus(responseStatus).setValue(responsePayload.value()).build();
    }
}
