package com.sirionlabs.api.EmailTokenAuthentication;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
public class EmailTokenAuthentication extends TestAPIBase {
    public static String getApiPath() {
        return "/emailTokenAuthentication";
    }
    public static APIResponse getResponse(String authToken)
    {
         String apiPath=getApiPath()+"?authToken="+authToken;
        APIResponse response = executor.getAPIWithoutMandatoryDefaultHeaders(apiPath,null).getResponse();
        return response;
    }

    public static String getEmailAuth(APIResponse response){
        String temp = response.getHeaders().get("Set-Cookie");
        int num1 = temp.indexOf("EmailAuthorization=")+19;
        int num2 = temp.indexOf("; Path");
        String emailAuth = temp.substring(num1, num2);

        return emailAuth;
    }
}
