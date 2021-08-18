package com.sirionlabs.test.sisenseBI;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.sisenseBI.AnalyticsServiceLogin;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;


public class TestAnalyticsServiceLogin {

    private final static Logger logger = LoggerFactory.getLogger(TestAnalyticsServiceLogin.class);

    private String username;
    private String password;

    @Test
    public void testAnalyticsServiceLogin(){

        CustomAssert csAssert = new CustomAssert();
        try {
            username = ConfigureEnvironment.getEnvironmentProperty("analyticsservice_username");
            password = ConfigureEnvironment.getEnvironmentProperty("analyticsservice_password");

        }catch (Exception e){
            logger.error("Exception while getting username and password");
            csAssert.assertTrue(false,"Exception while getting username and password");
        }
        String authtoken = Check.getAuthorization();
        String loginPayload = "{\"token\": \"" + authtoken + "\",\"susername\":\"" + username +"\",\"spassword\": \"" + password + "\"}";
        logger.info("Login with username [ {} ] and password [ {} ]",username,password);
        logger.info("Logging in Analytics Service with Payload " + loginPayload);

        AnalyticsServiceLogin analyticsServiceLogin = new AnalyticsServiceLogin();

        try{
            analyticsServiceLogin.hitAnalyticsServiceLogin(loginPayload);
            String statusCode = analyticsServiceLogin.getStatusCode();
            if(statusCode.contains("204")){
                logger.info("Analytics Service Logged in successfully");
                csAssert.assertTrue(true,"Analytics Service Logged in successfully");
            }else {
                logger.error("Analytics Service Logged in unsuccessfully");
                csAssert.assertTrue(false,"Analytics Service Logged in unsuccessfully");
            }
        }catch (Exception e){
            logger.error("Exception while hitting analytics login API " + e.getStackTrace());
            csAssert.assertTrue(false,"Exception while hitting analytics login API " + e.getStackTrace());
        }
        csAssert.assertAll();
    }
}