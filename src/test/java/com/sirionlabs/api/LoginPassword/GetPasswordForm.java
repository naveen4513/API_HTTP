package com.sirionlabs.api.LoginPassword;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class GetPasswordForm extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(GetPasswordForm.class);



    String queryPath = "/userPassword/form";

    public String getPasswordForm(String authToken){
        String hostUrl = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + ConfigureEnvironment.getEnvironmentProperty("Host") + ":" +
                ConfigureEnvironment.getEnvironmentProperty("Port");
        HashMap<String,String> headers = new HashMap<>();
        if(authToken!=null){
        headers.put("Authorization",authToken);
        }
        APIValidator response = executor.getWithoutAuthorization(hostUrl,queryPath,headers);
        return  response.getResponse().getResponseBody();


    }


    public String getPasswordFormwithInvalidPath(String authToken){
        String queryPath = "/userPassword/formtest";
        String hostUrl = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + ConfigureEnvironment.getEnvironmentProperty("Host") + ":" +
                ConfigureEnvironment.getEnvironmentProperty("Port");
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Authorization",authToken);
        APIValidator response = executor.getWithoutAuthorization(hostUrl,queryPath,headers);
        return  response.getResponse().getResponseBody();


    }


}
