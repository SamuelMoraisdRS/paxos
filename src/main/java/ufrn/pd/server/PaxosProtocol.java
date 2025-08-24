package ufrn.pd.server;

import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PaxosProtocol implements ApplicationProtocol {
    @Override
    public boolean validateRequest(String message) {
//        TODO: STUB
        return true;
    }

    @Override
    public String formatResponse(String message) {
//        TODO: STUB
        return message;
    }
}
