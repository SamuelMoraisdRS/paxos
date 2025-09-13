package ufrn.pd.client;

import io.grpc.ManagedChannelBuilder;
import ufrn.pd.gateway.GRPCMapper;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.service.user.protocol.ResponseStatus;

public class GRPCClient implements Client {
    private io.grpc.ManagedChannel channel;

    @Override
    public ResponsePayload sendAndReceive(String remoteAddress, int port, RequestPayload message) {
//        if (this.channel == null) {
//            this.channel = ManagedChannelBuilder.forAddress(remoteAddress, port).usePlaintext().build();
//        }
        io.grpc.ManagedChannel channel = ManagedChannelBuilder.forAddress(remoteAddress, port).usePlaintext().build();
        var stub = projetogrpc.GeneralServiceGrpc.newBlockingStub(channel);
        projetogrpc.Request request = GRPCMapper.toRequestMessage(message);
        try {
            var response = stub.sendRequest(request);
            return GRPCMapper.toResponsePayload(response);
        } catch (Exception e ) {
            System.err.println("Exception ao enviar mensagem");
//            e.printStackTrace();
            return new ResponsePayload(ResponseStatus.ERROR, "", message.destinationAddress());
        }
    }
}
