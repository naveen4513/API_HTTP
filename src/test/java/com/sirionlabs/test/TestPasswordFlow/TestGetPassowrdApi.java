package com.sirionlabs.test.TestPasswordFlow;

import com.sirionlabs.api.LoginPassword.GetPassword;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestGetPassowrdApi {

    private static String configFilePath;
    private static String configFileName;
    private static String sendEmailNo_UserEmail;
    private static String inactive_UserEmail;
    private static String sso_UserEmail;
    private static String blockingTime;
    private static String valid_UserEmail;

    GetPassword getPassword = new GetPassword();

    private final static Logger logger = LoggerFactory.getLogger(TestGetPassowrdApi.class);

    @BeforeClass
    public  void  before(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestPasswordFlowFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestPasswordFlowFileName");

        sendEmailNo_UserEmail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "sendemail_no_user");
        inactive_UserEmail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "inactive_user");
        sso_UserEmail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "sso_user");
        valid_UserEmail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "valid_user");
        blockingTime = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "blockingtime");
    }


    @Test(description = "C153035", priority = 0)
    public void invlidApiMethod(){
        CustomAssert csAssert = new CustomAssert();
        APIValidator response = getPassword.getPasswordInvalidMethod();

            csAssert.assertEquals(response.getResponse().getResponseCode(),Integer.valueOf(405)
                    ,"Expected status code 405  but actual status code "+response.getResponse().getResponseCode());

            csAssert.assertAll();
    }

    @Test(description = "C153038", priority = 1)
    public void invlidApiPath(){
        CustomAssert csAssert = new CustomAssert();
        APIValidator response = getPassword.getPasswordInvalidPATH();

        csAssert.assertFalse(JSONUtility.validjson(response.getResponse().getResponseBody())
                ,"APi response Body is " + response.getResponse().getResponseBody());
        csAssert.assertAll();

    }

    @Test(description = "C153037", priority = 2)
    public void EmailNotExist(){
        CustomAssert csAssert = new CustomAssert();
        String response = getPassword.getPassword("testauto!@#$%^&@sirionqa.office");

        csAssert.assertEquals(JSONUtility.parseJson(response,"$.errors[0].defaultMessage"),
                "Instructions to reset password will be sent to your registered email ID. In case email not received, please check your email ID or reach out to administrator/support team."
                ,"Response in case of invalid body is "+response);
        csAssert.assertAll();

    }

    @Test(description = "C153036", priority = 3)
    public void invalidBody(){
        CustomAssert csAssert = new CustomAssert();
        String response = getPassword.getPasswordInvalidBody("testauto@sirionqa.office");

        csAssert.assertEquals(JSONUtility.parseJson(response,"$.errorMessage"),
                "Required request parameter data is either missing or wrong."
                ,"Response in case of invalid body is "+response);
        csAssert.assertAll();

    }

   // @Test(description = "", priority = 4)
    public void TestSSOUser(){
        CustomAssert csAssert = new CustomAssert();
        String response = getPassword.getPassword(sso_UserEmail);

        csAssert.assertEquals(JSONUtility.parseJson(response,"$.message"),
                "Please login using the Single Sign-On method prescribed by your employer."
                ,"Response in case of SSO user is "+response);
        csAssert.assertAll();

    }

    @Test(description = "", priority = 5)
    public void TestInactiveUser(){
        CustomAssert csAssert = new CustomAssert();
        String response = getPassword.getPassword(inactive_UserEmail);

        csAssert.assertEquals(JSONUtility.parseJson(response,"$.errors[0].defaultMessage"),
                "Your account is not active. Please contact your administrator."
                ,"Response in case of Inactive user is "+response);
        csAssert.assertAll();
    }

   // @Test(description = "", priority = 6)
    public void TestSendEmailNo() {
        CustomAssert csAssert = new CustomAssert();
        String response = getPassword.getPassword(sendEmailNo_UserEmail);
        csAssert.assertEquals(JSONUtility.parseJson(response, "$.message"),
                "Password reset failed. No e-mail could be sent to user as send e-mail is false. Please contact your administrator"
                , "Response in case of SendEmail No is " + response);

    }


   // @Test(description = "C153034", priority = 7) covered in TestGetApi Testcase of TestGetPasswordForm class
    public void TestGetApi() {
        CustomAssert csAssert = new CustomAssert();
        String response = getPassword.getPassword(valid_UserEmail);
        csAssert.assertEquals(JSONUtility.parseJson(response, "$.message"),
                "Please check your e-mail for further instructions."
                , "Response in valid case is  " + response);
        csAssert.assertAll();

    }

    @Test(description = "C153034", priority = 8)
    public void TestBlockUserForRetrigger() {
        CustomAssert csAssert = new CustomAssert();
        getPassword.getPassword(valid_UserEmail);
        String response = getPassword.getPassword(valid_UserEmail);
        csAssert.assertEquals(JSONUtility.parseJson(response,"$.errors[0].defaultMessage"),
                "Your request couldn’t be processed as you have already submitted request in last "+blockingTime+". Please try after some time or contact admin."
                ,"Response in case of Inactive user is "+response);
        csAssert.assertAll();
    }




}
