package ufrn.pd.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ufrn.pd.gateway.GRPCMapper;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.service.user.protocol.ResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GRPCClient implements Client {
    private io.grpc.ManagedChannel channel;

    private final Map<NodeAddress, ManagedChannel> channelsPerConnection = new ConcurrentHashMap<>();

    @Override
    public ResponsePayload sendAndReceive(String remoteAddress, int port, RequestPayload message) {
        NodeAddress address = new NodeAddress(remoteAddress, port);
        if (!channelsPerConnection.containsKey(address)) {
            channelsPerConnection.put(address, ManagedChannelBuilder.forAddress(remoteAddress, port).usePlaintext().build());
        }

        io.grpc.ManagedChannel channel = channelsPerConnection.get(address);

        var stub = projetogrpc.GeneralServiceGrpc.newBlockingStub(channel);

        projetogrpc.Request request = GRPCMapper.toRequestMessage(message);
        try {
            var response = stub.sendRequest(request);
            return GRPCMapper.toResponsePayload(response);
        } catch (Exception e ) {
            System.err.println("Exception ao enviar mensagem");
            return new ResponsePayload(ResponseStatus.ERROR, "", message.destinationAddress());
        }
    }
}
