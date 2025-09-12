package ufrn.pd.server;

import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import projetogrpc.NodeAddressGRPC;
import projetogrpc.Response;
import projetogrpc.ResponseStatus;
import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.Service;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;

import java.io.IOException;
import java.util.Optional;

public class GRPCServerImpl implements Server {

    private final int port;

    public GRPCServerImpl(int port) {
        this.port = port;
    }

    @Override
    public void runServer(Service service) {
        int numAttempts = 5;
        for (int i = 1; i <= numAttempts; i++) {
            try {
                var serverBuilder = ServerBuilder.forPort(port).
                        addService(new GeneralServer(service)).build().start();
                serverBuilder.awaitTermination();
            } catch (IOException e) {
                System.err.println("ServerGRPCImpl - Error starting server: " + e.getMessage() + " - Attempt " + i);
            } catch (InterruptedException e) {
                System.err.println("ServerGRPCImpl - The Server has been shutdown: " + e.getMessage());
                return;
            }

        }
    }

    class GeneralServer extends projetogrpc.GeneralServiceGrpc.GeneralServiceImplBase {
        private final Service service;

        public GeneralServer(Service service) {
            this.service = service;
        }

        @Override
        public void sendRequest(projetogrpc.Request request, StreamObserver<Response> responseObserver) {
            NodeAddress requestDestinationAddress = new NodeAddress(request.getAddress().getIp(), request.getAddress().getPort());
            NodeRole requestSenderRole = NodeRole.valueOf(request.getSenderRole().toString());
            NodeRole requestDestinationRole = NodeRole.valueOf(request.getDestinationRole().toString());
            RequestPayload requestPayload = new RequestPayload(requestDestinationAddress, requestSenderRole,
                    requestDestinationRole, request.getOperation(), request.getValue());
            Optional<ResponsePayload> responsePayload = Optional.ofNullable(service.handle(requestPayload));

            if (responsePayload.isEmpty()) {
                // TODO : Send error message
            }
            NodeAddressGRPC responseSenderAddress = NodeAddressGRPC.newBuilder().
                    setIp(responsePayload.get().senderAddress().ip()).
                    setPort(responsePayload.get().senderAddress().port()).build();

            ResponseStatus responseStatus = ResponseStatus.valueOf(responsePayload.get().status().toString());

            var response = projetogrpc.Response.newBuilder().setSenderAddress(responseSenderAddress).
                    setStatus(responseStatus).setValue(responsePayload.get().value()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
