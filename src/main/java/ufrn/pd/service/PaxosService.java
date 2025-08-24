package ufrn.pd.service;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PaxosService implements Service{
    private final ConcurrentHashMap<Integer, Double> DATABASE = new ConcurrentHashMap<>();

    @Override
    public String handle(String request) {
        return request;
//        String [] values = request.split(":");
//        String operation = values[0];
//        String payload = values[1];
//        if (operation.equals("STORE")) {
//            return store(payload);
//        } else if (operation.equals("READ")) {
//            return read(Integer.parseInt(payload));
//        } else if (operation.equals("SHOW")) {
//           return "SHOW";
//        }
//        return "ERROR";
    }

    private String store(String payload) {
        String [] values = payload.split(",");
        DATABASE.put(Integer.parseInt(values[0]), Double.parseDouble(values[1]));
        return "STORE-OK:id=" + values[0] + ",value=" + values[1];
    }

    private String read(Integer key) {
        Double value = DATABASE.get(key);
        return "READ-OK:id=" + key + ",value=" + value;
    }
}
