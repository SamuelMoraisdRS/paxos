package ufrn.pd.service.user.protocol;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.utils.protocol.HTTPProtocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HTTPUserProtocol extends HTTPProtocol {
    // The HTTP methods that are valid for each resource
    private static final Map<String, String> methodPerResource = new HashMap<>();

    static {
        methodPerResource.put("RETRIEVE", "GET");
        methodPerResource.put("CREATE", "POST");
    }

    @Override
    public boolean validateOperation(String method, String resource) {
//        Optional<String> validMethod = Optional.ofNullable(methodPerResource.get(resource));
//        return validMethod.isPresent() && validMethod.get().equalsIgnoreCase(method);
        return true;
    }

    @Override
    public Optional<RequestPayload> validateMessage(String msg) {
        return Optional.empty();
    }
}
