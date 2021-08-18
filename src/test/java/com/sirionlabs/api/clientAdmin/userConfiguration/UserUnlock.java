package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.apache.kafka.common.protocol.types.Field;

import java.util.HashMap;

public class UserUnlock extends TestAPIBase {

    public static String getAPIPath(int userID){
        return "/resetlogincount/" + userID;
    }

    public static HashMap<String, String> getHeaders(){
        HashMap<String, String> headers = ApiHeaders.getDefaultAcceptEncodingHeader();
        headers.put("Accept", "*/*");
        return headers;
    }

    public static int hitUsersUnlockAPI(int userID){
        return executor.get(getAPIPath(userID), getHeaders()).getResponse().getResponseCode();
    }
}
