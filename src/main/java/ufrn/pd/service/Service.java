package ufrn.pd.service;

import ufrn.pd.service.user.RequestPayload;
import ufrn.pd.service.user.ResponsePayload;

/*
Interface that represent the app's service logic. The handle method will be called the server
 */
public interface Service {
    // THe method to handle a request. In the current implementation, this is a blocking operation
    ResponsePayload handle(RequestPayload request);
}
