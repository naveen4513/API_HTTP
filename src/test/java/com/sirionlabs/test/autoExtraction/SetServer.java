package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.clientAdmin.SwitchServer;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class SetServer {

    private final static Logger logger = LoggerFactory.getLogger(SetServer.class);
     String configAutoExtractionFilePath;
     String configAutoExtractionFileName;
     String clientId;
     String userId;

    @BeforeClass
    public void beforeClass() {
        CustomAssert csAssert = new CustomAssert();

        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        clientId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "clientid");
        userId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "userid");

        //Login to Client Admin
        try {
            Check check = new Check();
            // Login to Client Admin
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            csAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");
        }
        catch (Exception e)
        {
            logger.info("Error occured while applying duplicate Data Filter");
            csAssert.assertTrue(false,e.getMessage());
        }
    }



    @Parameters("Server")
    @Test
    public void setServer(String Server) {
        CustomAssert csAssert = new CustomAssert();

        if(System.getProperty("Server") != null) {
            Server = System.getProperty("Server");
        }

        if (Server.equals("A_Server")) {

            //Now switching to Server A
            try{
                logger.info("Switching to Server A");
                HttpResponse originalServerResponse = SwitchServer.originalVersion(userId);
                csAssert.assertTrue(originalServerResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String originalServerStr = EntityUtils.toString(originalServerResponse.getEntity());
                csAssert.assertTrue(originalServerStr.contains("true"),"Not switching to server A");
            }
            catch (Exception e)
            {
                logger.info("Error occured after while hitting original API");
                csAssert.assertTrue(false,e.getMessage());
            }

        } else if (Server.equals("B_Server")) {

            //Now switching to Server B
            try {
                logger.info("Switching to Server B");
                HttpResponse customServerResponse = SwitchServer.customVersion(userId);
                csAssert.assertTrue(customServerResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String customServerStr = EntityUtils.toString(customServerResponse.getEntity());
                csAssert.assertTrue(customServerStr.contains("true"), "Not switching to server B");
            } catch (Exception e) {
                logger.info("Error occured after while hitting original API");
                csAssert.assertTrue(false, e.getMessage());
            }
        }
        csAssert.assertAll();
    }

    @AfterClass
    public void loginToEndUser()
    {
        CustomAssert csAssert = new CustomAssert();
        Check check = new Check();
        // Login to End User
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
        HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
        csAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
        logger.info("User has been logged in as an end user");
        csAssert.assertAll();

    }
}


