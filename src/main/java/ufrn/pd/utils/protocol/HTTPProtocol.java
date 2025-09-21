package ufrn.pd.utils.protocol;


import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.service.user.protocol.ResponseStatus;

import java.util.*;

// TODO : DUas solucoes: Trazer a validacao para a camada de servico ou acoplar a camada de servico a de protocolo
// TODO : Vamos fazer a primeira
public abstract class HTTPProtocol implements ApplicationProtocol {
    private static final Map<ResponseStatus, String> CODES = new HashMap<>();

    static {
        CODES.put(ResponseStatus.OK,"200 Ok");
        CODES.put(ResponseStatus.SERVICE_NOT_FOUND, "404 Not Found");
        CODES.put(ResponseStatus.ERROR,"500 Internal Server Error");
    }

    static enum HTTPMethod {
        GET, POST, PUT, DELETE;
    }

    @Override
    public Optional<RequestPayload> validateMessage(String msg) {

        String header = msg.split("\n\n")[0];
        return Optional.empty();
    }


    // TODO : The HTTP resource will be our DestinationRole, and the HTTP method will be our operation
    private String [] parseHeader(String headerContent) throws Exception {
        String [] headerLines = headerContent.split("\n");
        String [] methodLine = headerLines[0].split(" ");
        // TODO : Add exception handling
        String destinationRole = methodLine[1].substring(1).toUpperCase();
        String senderRole = headerLines[2].split(" ")[1].toUpperCase();
        return new String [] {methodLine[0], destinationRole, senderRole};
    }

    // This method will convert the Method + resource (operation) of the header of an http request into a suitable
    // operation of the target service
    public abstract boolean validateOperation(String method, String resource);

    @Override
    public RequestPayload parseRequest(String message) {
        System.out.println("Recebido Service: " + message);
        // So we can obtain each line from the message (request or response)
        String [] splitMessage = message.split("\n\n");
        String header = null;
        String body = null;
        try {
            header = splitMessage[0];
            body = splitMessage[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO : Use 400 message
           return new RequestPayload(null, null, null, "ERROR - ERROR", "Malformed HTTP message");
        }
        String [] headerLines = header.split("\n");
        String [] methodLine = headerLines[0].split(" ");
        // TODO : Add exception handling
        // Parses the resource section of the header line to obtai the destination service and the operation to be performed
        String httpMethod = methodLine[0];
        String [] resource = methodLine[1].substring(1).split("/");
        // TODO : Perform validation regarding hte destination address and the destination role. This is validation performed
        // through all of the nodes, maybe using an interceptor we can give it a single source of truth
        String destination = resource[0].toUpperCase();
        String operation = resource[1].toUpperCase();
        String sender = headerLines[2].split(" ")[1].toUpperCase();
        NodeRole senderRole = NodeRole.valueOf(sender);
        NodeRole destinationRole = NodeRole.valueOf(destination);
        if (!validateOperation(httpMethod, operation)){
            return new RequestPayload(null, senderRole, destinationRole, "ERROR - NOT FOUND", "Invalid operation");
        }
        return new RequestPayload(null, senderRole, destinationRole, operation, body);
    }

    public ResponsePayload parseResponse(String message) {
        System.out.println("Recebido no Service: " + message);
        String [] msg = message.split("\n");
        ResponseStatus status = switch (msg[0].split(" ")[1]) {
            case "200":
                yield ResponseStatus.OK;
            case "404":
                yield ResponseStatus.SERVICE_NOT_FOUND;
            case "500":
                yield ResponseStatus.ERROR;
            default:
                throw new IllegalStateException("Unexpected value: " + msg[0].split(" ")[1]);
        };
        NodeAddress senderAddress = NodeAddress.fromString(msg[1].split(" ")[1]);
        // ! We're ignoring the other headers, but they mght be useful later
        String body = msg[5];
        return new ResponsePayload(status, body, senderAddress);
    }

//    abstract String mapOperationToHttpMethod(String operation);

    // TODO : maybe rename to createRequest
    @Override
    public String createRequest(RequestPayload message) {
        StringBuilder stringBuilder = new StringBuilder();

        // Garante que o resource sempre tenha "/" no início e seja minúsculo
        String destinationRole = String.format("/%s/%s", message.destinationRole().toString().toLowerCase(),
                message.operation().toLowerCase());

        // Primeira linha: METHOD PATH HTTP/1.1
        String requestLine = String.format("%s %s HTTP/1.1",
                message.operation().toUpperCase(),
                destinationRole);
        stringBuilder.append(requestLine).append("\r\n");

        // TODO : Remove stub
        stringBuilder.append(String.format("Host: %s\r\n", "STUB"));

        stringBuilder.append(String.format("User-Agent: %s\r\n", message.senderRole()));
        stringBuilder.append("Content-Type: text\r\n\r\n");
        stringBuilder.append(message.value() + "\r\n");
        return stringBuilder.toString();
    }
    @Override
    public String createResponse(ResponsePayload message) {
        // TODO : STUB
        StringBuilder stringBuilder = new StringBuilder();
        String statusCode = CODES.get(message.status());
        stringBuilder.append("HTTP/1.1 " + statusCode + "\r\n");
        stringBuilder.append("Server: " + message.senderAddress() + "\r\n");
        stringBuilder.append("Content-Type: text\r\n");
        String date = String.format("Date: %s\r\n", new Date());
        stringBuilder.append(date);

        stringBuilder.append("\r\n");

        stringBuilder.append(message.value() + "\r\n");

        return stringBuilder.toString();

    }
}
