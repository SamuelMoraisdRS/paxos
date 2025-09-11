package ufrn.pd.utils.protocol;


import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;
import ufrn.pd.service.user.protocol.ResponseStatus;

import java.util.*;

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

//    public Optional<RequestPayload> validateMessage(List<String> msg) {
//        return Optional.empty();
//    }


    // TODO : The HTTP resource will be our DestinationRole, and the HTTP method will be our operation
    private String [] parseHeader(String headerContent) {
        String [] headerLines = headerContent.split("\n");
        String [] methodLine = headerLines[0].split(" ");
        // TODO : Be wary of this
        // TODO : Add exception handling
//        HTTPMethod operation = HTTPMethod.valueOf(methodLine[0]);
        // Removes the '/' and converts the string to upper case
        String destinationRole = methodLine[1].substring(1).toUpperCase();
        // TODO : We'll consider that client will be defined on the user agent field
        String senderRole = headerLines[2].split(" ")[1].toUpperCase();
        return new String [] {methodLine[0], destinationRole, senderRole};
    }

    @Override
    public RequestPayload parseRequest(String message) {
        System.out.println("Recebidoservvice: " + message);
        // So we can obtain each line from the message (request or response)
        String[] splitMessage = message.split("\n\n");
        String header = splitMessage[0];
        String body = splitMessage[1];
        String [] headerData = parseHeader(header);
        NodeRole senderRole = NodeRole.valueOf(headerData[2]);
        NodeRole destinationRole = NodeRole.valueOf(headerData[1]);
        return new RequestPayload(null, senderRole, destinationRole, headerData[0], body);
    }

    public ResponsePayload parseResponse(String message) {
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
        String destinationRole = String.format("/%s", message.destinationRole().toString().toLowerCase());

        // Primeira linha: METHOD PATH HTTP/1.1
        String requestLine = String.format("%s %s HTTP/1.1",
                message.operation().toUpperCase(),
                destinationRole);
        stringBuilder.append(requestLine).append("\r\n");

        // TODO : Remove stub
        stringBuilder.append(String.format("Host: %s\r\n", "STUB"));

        stringBuilder.append(String.format("User-Agent: %s\r\n", message.senderRole()));

//        if (message.value() != null && !message.value().isEmpty()) {
//            stringBuilder.append("Content-Type: application/json\r\n");
//            // TODO : We're performing decoding based on string lines so this might be useless. Use byte decoding
//            stringBuilder.append(String.format("Content-Length: %d\r\n", message.value().getBytes().length));
//        }
//        stringBuilder.append("\r\n");
//        if (message.value() != null && !message.value().isEmpty()) {
//            stringBuilder.append(message.value() + "\r\n");
//        }

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
