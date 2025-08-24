package ufrn.pd.server;

/*
Class that will represent the application's protocol, performing parsing and generating responses
 */
public interface ApplicationProtocol {
    boolean validateRequest(String message);


    String formatResponse(String message);
}
