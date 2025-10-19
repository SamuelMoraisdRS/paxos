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

        ManagedChannel channel = channelsPerConnection.compute(address, (addr, existing) -> {
            if (existing == null || existing.isShutdown() || existing.isTerminated() ) {
//                System.out.println("Criando novo canal para " + addr);
                return ManagedChannelBuilder.forAddress(remoteAddress, port)
                        .usePlaintext()
                        .build();
            }
            return existing;
        });
        var stub = projetogrpc.GeneralServiceGrpc.newBlockingStub(channel);
        var request = GRPCMapper.toRequestMessage(message);

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
