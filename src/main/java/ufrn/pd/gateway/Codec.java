package ufrn.pd.gateway;

import java.io.BufferedReader;

public interface Codec {
    String decodeMessage(BufferedReader reader);
}
