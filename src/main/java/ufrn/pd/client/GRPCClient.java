package ufrn.pd.client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ufrn.pd.gateway.GRPCMapper;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.service.bm25service.RequestPayload;
import ufrn.pd.service.bm25service.ResponsePayload;
import ufrn.pd.utils.protocol.ResponseStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GRPCClient implements Client {
    private io.grpc.ManagedChannel channel;

    // dict with the Channels for each node (only the gateway has more than one node connection)
    private final Map<NodeAddress, ManagedChannel> channelsPerConnection = new ConcurrentHashMap<>();

    @Override
    public ResponsePayload sendAndReceive(RequestPayload message) {
        NodeAddress address = message.destinationAddress();
        String remoteAddress = address.ip();
        int port = address.port();

        io.grpc.ManagedChannel channel = channelsPerConnection.get(address);

        var stub = projetogrpc.GeneralServiceGrpc.newBlockingStub(channel);

        try {
            var response = stub.sendRequest(request);
            return GRPCMapper.toResponsePayload(response);
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            channel.shutdownNow();
            channelsPerConnection.remove(address);
            return new ResponsePayload(ResponseStatus.ERROR, "", address);
        }
    }
}
