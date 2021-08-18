package com.sirionlabs.api.LoginPassword;

import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class GetPassword extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(GetPassword.class);

    String queryPath = "/v3/getpassword";


       public String getPassword(String emailID){

        String payload = " {\"email\":\""+emailID+"\"} ";
        HashMap<String,String> headers = ApiHeaders.getDefaultLegacyHeaders();
        APIValidator response = executor.post(queryPath, headers, payload, null);

        if(response.getResponse().getResponseCode()==200){
            return response.getResponse().getResponseBody() ;

        }else{
            logger.error("get Password Api status code is "+response.getResponse().getResponseCode() );
            return "";
        }

    }

    public String getPasswordInvalidBody(String emailID){

        String payload = " {\"emailddd\":\""+emailID+"\"} ";
        HashMap<String,String> headers = ApiHeaders.getDefaultLegacyHeaders();
        APIValidator response = executor.post(queryPath, headers, payload, null);

        if(response.getResponse().getResponseCode()==400){
            return response.getResponse().getResponseBody() ;

        }else{
            logger.error("get Password Api status code is "+response.getResponse().getResponseCode() );
            return "";
        }

    }


    public APIValidator getPasswordInvalidMethod(){

        HashMap<String,String> headers = ApiHeaders.getDefaultLegacyHeaders();
        return executor.get(queryPath, headers);
    }

    public APIValidator getPasswordInvalidPATH(){
        String invalidQueryPath = "/v3/getpasswordtest";
        HashMap<String,String> headers = ApiHeaders.getDefaultLegacyHeaders();
        return executor.get(invalidQueryPath, headers);
    }


}
