package ufrn.pd.utils.protocol;


import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.user.dtos.RequestPayload;

import java.util.*;

public abstract class HTTPProtocol implements ApplicationProtocol {
    private static final Map<String, String> CODES = new HashMap<>();

    static {
        CODES.put("200", "Ok");
        CODES.put("404", "Not Found");
        CODES.put("500", "Internal Server Error");
        CODES.put("403", "Forbidden");
    }

    static enum HTTPMethod {
        GET, POST, PUT, DELETE;
    }

    public  Optional<RequestPayload> validateMessage(List<String> msg) {
        return Optional.empty();
    }

    // TODO : The HTTP resource will be our DestinationRole, and the HTTP method will be our operation
    private String [] parseHeader(String headerContent) {
        String [] headerLines = headerContent.split("\n");
        String [] methodLine = headerLines[0].split(" ");
        // TODO : Be wary of this
        // TODO : Add exception handling
        HTTPMethod operation = HTTPMethod.valueOf(methodLine[0]);
        // Removes the '/' and converts the string to upper case
        String destinationRole = methodLine[1].substring(1).toUpperCase();
        // TODO : We'll consider that client will be defined on the user agent field
        String senderRole = headerLines[2].split(" ")[1].toUpperCase();
        return new String [] {operation.toString(), destinationRole, senderRole};
    }

    @Override
    public RequestPayload parse(String message) {
        // So we can obtain each line from the message (request or response)
        String[] splitMessage = message.split("\n\n");
        String header = splitMessage[0];
        String body = splitMessage[1];
        String [] headerData = parseHeader(header);
        NodeRole senderRole = NodeRole.valueOf(headerData[2]);
        NodeRole destinationRole = NodeRole.valueOf(headerData[1]);
        return new RequestPayload(null, senderRole, destinationRole, headerData[0], body);
    }
//    // TODO : The nodes should be able to create requests and responses
//    @Override
//    // TODO : Perhaps rename to createRequest
//    public String createMessage(RequestPayload message) {
//        // TODO : We'll make so the resource is in lower case and always prepended with a '/'
//        StringBuilder stringBuilder = new StringBuilder();
//        String destinationRole = String.format("/%s", message.destinationRole().toString().toLowerCase());
//        String headerLine = String.format("%s %s HTTP/1.1", message.operation().toUpperCase(), destinationRole, message.senderRole());
//
//        stringBuilder.append(headerLine);
//
//        return stringBuilder.toString();
//    }
//
    @Override
// TODO : Perhaps rename to createRequest
    public String createMessage(RequestPayload message) {
        StringBuilder stringBuilder = new StringBuilder();

        // Garante que o resource sempre tenha "/" no início e seja minúsculo
        String destinationRole = String.format("/%s", message.destinationRole().toString().toLowerCase());

        // Primeira linha: METHOD PATH HTTP/1.1
        String requestLine = String.format("%s %s HTTP/1.1",
                message.operation().toUpperCase(),
                destinationRole);
        stringBuilder.append(requestLine).append("\r\n");

        // Host header é obrigatório em HTTP/1.1
        // TODO : Remove stub
        stringBuilder.append(String.format("Host: %s\r\n", "stub"));

        stringBuilder.append(String.format("User-Agent: %s\r\n", message.senderRole()));

        if (message.value() != null && !message.value().isEmpty()) {
            stringBuilder.append("Content-Type: application/json\r\n");
            // TODO : We're doing decoding based on string lines so this might be useless. Use byte decoding
            stringBuilder.append(String.format("Content-Length: %d\r\n", message.value().getBytes().length));
        }
        stringBuilder.append("\r\n");
        if (message.value() != null && !message.value().isEmpty()) {
            stringBuilder.append(message.value());
        }
        return stringBuilder.toString();
    }
    public String createResponse(RequestPayload message) {
        // TODO : STUB
        return "";

    }
}
