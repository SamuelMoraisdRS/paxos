
package ufrn.pd.utils.protocol;

import ufrn.pd.gateway.NodeAddress;
import ufrn.pd.gateway.NodeRole;
import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;

import java.util.*;

public abstract class HTTPProtocol implements ApplicationProtocol {
    private static final Map<ResponseStatus, String> CODES = new HashMap<>();

    static {
        CODES.put(ResponseStatus.OK,"200 OK");
        CODES.put(ResponseStatus.SERVICE_NOT_FOUND, "404 Not Found");
        CODES.put(ResponseStatus.ERROR,"500 Internal Server Error");
    }

    enum HTTPMethod {
        GET, POST, PUT, DELETE
    }

    @Override
    public Optional<RequestPayload> validateMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return Optional.empty();
        }
        try {
            parseRequest(msg); // tenta parsear
            return Optional.ofNullable(parseRequest(msg));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public RequestPayload parseRequest(String message) {
        System.out.println("Recebido Service: " + message);

        String[] splitMessage = message.split("\n\n", 2);
        if (splitMessage.length < 1) {
            return new RequestPayload(null, null, null,
                    "ERROR", "Malformed HTTP message (no header)");
        }

        String header = splitMessage[0];
        String body = splitMessage.length > 1 ? splitMessage[1] : "";

//        String[] headerLines = header.split("\r\n");
        String[] headerLines = header.split("\n");
        if (headerLines.length < 1) {
            return new RequestPayload(null, null, null,
                    "ERROR", "Malformed HTTP message (no request line)");
        }

        // Primeira linha → METHOD PATH HTTP/1.1
        String[] methodLine = headerLines[0].split(" ");
        if (methodLine.length < 2) {
            return new RequestPayload(null, null, null,
                    "ERROR", "Malformed request line");
        }

        String httpMethod = methodLine[0];
        String[] resourceParts = methodLine[1].substring(1).split("/");
        if (resourceParts.length < 2) {
            return new RequestPayload(null, null, null,
                    "ERROR", "Invalid resource path");
        }

        String destination = resourceParts[0].toUpperCase();
        String operation = resourceParts[1].toUpperCase();

        // Recupera o sender
        NodeRole senderRole = null;
        for (String line : headerLines) {
            if (line.startsWith("User-Agent:")) {
                String userAgent = line.split(" ")[1];
                // TODO Move this into the ApiGateway http protocol implementation
                if (userAgent.toLowerCase().contains("client")){
                    senderRole = NodeRole.CLIENT;
                } else {
                    senderRole = NodeRole.valueOf(line.split(" ")[1].toUpperCase());
                }

                break;
            }
        }
        if (senderRole == null) {
            return new RequestPayload(null, null, null,
                    "ERROR", "Missing sender role");
        }

        NodeRole destinationRole = NodeRole.valueOf(destination);

        if (!validateOperation(httpMethod, operation)) {
            return new RequestPayload(null, senderRole, destinationRole,
                    "ERROR - NOT FOUND", "Invalid operation");
        }

        return new RequestPayload(null, senderRole, destinationRole, operation, body.trim());
    }

    public ResponsePayload parseResponse(String message) {
//        System.out.println("Recebido no Service: " + message);

        String[] splitMessage = message.split("\n\n", 2);
        String header = splitMessage[0];
        String body = splitMessage.length > 1 ? splitMessage[1] : "";

        String[] headerLines = header.split("\r\n");
        if (headerLines.length < 1) {
            throw new IllegalStateException("Malformed HTTP response");
        }

        String[] statusLine = headerLines[0].split(" ");
        if (statusLine.length < 2) {
            throw new IllegalStateException("Malformed status line");
        }

        ResponseStatus status = switch (statusLine[1]) {
            case "200" -> ResponseStatus.OK;
            case "404" -> ResponseStatus.SERVICE_NOT_FOUND;
            case "500" -> ResponseStatus.ERROR;
            default -> throw new IllegalStateException("Unexpected status code: " + statusLine[1]);
        };

        NodeAddress senderAddress = null;
        for (String line : headerLines) {
            if (line.startsWith("Server:")) {
                senderAddress = NodeAddress.fromString(line.split(" ")[1]);
                break;
            }
        }

        System.out.println("parse_response : Mensagem recebida: " + message);
        System.out.println("parse_response : Body recebido: " + body);

        return new ResponsePayload(status, body.trim(), senderAddress);
    }

    @Override
    public String createRequest(RequestPayload message) {
        StringBuilder sb = new StringBuilder();

        // Exemplo: GET /user/create HTTP/1.1
        String destinationPath = String.format("/%s/%s",
                message.destinationRole().toString().toLowerCase(),
                message.operation().toLowerCase());

        String requestLine = String.format("%s %s HTTP/1.1",
                HTTPMethod.POST, destinationPath); // aqui você pode decidir GET/POST dinamicamente
        sb.append(requestLine).append("\n");

        sb.append("Host: localhost\r\n");
        sb.append(String.format("User-Agent: %s\r\n", message.senderRole()));
        sb.append("Content-Type: text/plain\r\n");
        sb.append("Content-Length: ").append(message.value().length()).append("\r\n");
        sb.append("\r\n");
        sb.append(message.value()).append("\r\n");
        return sb.toString();
    }

    @Override
    public String createResponse(ResponsePayload message) {
        StringBuilder sb = new StringBuilder();
        String statusCode = CODES.get(message.status());
//        System.out.println("Response payload utilizado: " + message);
        sb.append("HTTP/1.1 ").append(statusCode).append("\r\n");
        sb.append("Server: ").append(message.senderAddress()).append("\r\n");
        sb.append("Content-Type: text/plain\n");
        sb.append("Date: ").append(new Date()).append("\r\n");
        sb.append("Content-Length: ").append(message.value().length()).append("\n");
        sb.append("\n");
        sb.append(message.value()).append("\r\n");

        return sb.toString();
    }

    public abstract boolean validateOperation(String method, String resource);
}