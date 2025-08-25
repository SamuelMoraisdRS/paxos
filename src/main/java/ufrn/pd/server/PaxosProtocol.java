package ufrn.pd.server;


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
