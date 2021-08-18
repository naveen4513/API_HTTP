package com.sirionlabs.test.TestPasswordFlow;

import com.sirionlabs.api.LoginPassword.GetPassword;
import com.sirionlabs.api.LoginPassword.GetPasswordForm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;



public class TestGetPasswordForm {

    private final static Logger logger = LoggerFactory.getLogger(TestGetPasswordForm.class);

    private static String configFilePath;
    private static String configFileName;
    private static String valid_UserEmail;

    GetPasswordForm form = new GetPasswordForm();
    GetPassword getPassword = new GetPassword();
    LoginUtility loginUtil = new LoginUtility();

    @BeforeClass
    public  void  before(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestPasswordFlowFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestPasswordFlowFileName");
        valid_UserEmail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "valid_user");

    }

    @Test(description = "C153042", priority = 0)
    public void TestAuthTokenInvalid() {
        CustomAssert csAssert = new CustomAssert();
        String formResponse = form.getPasswordForm("test");
        if(JSONUtility.validjson(formResponse)){
            csAssert.assertTrue(false,"get password form api response is valid json " +
                    "in case of invalid auth token "+ formResponse);
        }
        csAssert.assertAll();


    }

    @Test(description = "C153043", priority = 1)
    public void TestAuthTokenNotPassed() {
        CustomAssert csAssert = new CustomAssert();
        String formResponse = form.getPasswordForm(null);
        if(JSONUtility.validjson(formResponse)){
            csAssert.assertTrue(false,"get password form api response is valid json " +
                    "in case of invalid auth token "+ formResponse);
        }

            csAssert.assertAll();
    }

    //@Test(description = "C153041", priority = 2)
    public void TestAuthInvalidPath() {
        CustomAssert csAssert = new CustomAssert();
        loginUtil.deleteEntryFromTable(valid_UserEmail,"Reset Your Sirion Login Password");
        String response = getPassword.getPassword(valid_UserEmail);

        try {
            if(((String)JSONUtility.parseJson(response, "$.message"))
                    .contains("Please check your e-mail for further instructions.")){
                String authToken = loginUtil.getPasswordEmailTokenfromEmail(valid_UserEmail,"Reset Your Sirion Login Password",csAssert);
                String formResponse = form.getPasswordFormwithInvalidPath(authToken);
                if(JSONUtility.validjson(formResponse)){
                    csAssert.assertEquals( JSONUtility.parseJson(formResponse,"$.errorMessage"),"Signature did not match with last authentication."
                            ,"response for invalid path for get password form api is "+ formResponse);}
                else{
                    csAssert.assertTrue(false,"Exception while getting response of " +
                            "userPassword/form API");
                }

            }else{
                csAssert.assertTrue(false,"Response of get password API is " + response);
            }

        }catch (Exception e){
            csAssert.assertTrue(false,"Response of get password API is " + response);
        }
        csAssert.assertAll();


    }


    @Test(description = "C153039", priority = 3)
    public void TestGetApi() {
        CustomAssert csAssert = new CustomAssert();
        loginUtil.deleteEntryFromTable(valid_UserEmail,"Reset Your Sirion Login Password");
        String response = getPassword.getPassword(valid_UserEmail);

        try {
            if(((String)JSONUtility.parseJson(response, "$.message"))
                    .contains("Instructions to reset password will be sent to your registered email ID. In case email not received, please check your email ID or reach out to administrator/support team.")){
                String authToken = loginUtil.getPasswordEmailTokenfromEmail(valid_UserEmail,"Reset Your Sirion Login Password",csAssert);
                String formResponse = form.getPasswordForm(authToken);
                if(JSONUtility.validjson(formResponse)){
                csAssert.assertEquals( JSONUtility.parseJson(formResponse,"$.email"),valid_UserEmail
                        ,"email is not correct on the reset form page. Expected "+ valid_UserEmail
                +"Actual "+ JSONUtility.parseJson(formResponse,"$.email"));}
                else{
                    csAssert.assertTrue(false,"Exception while getting response of " +
                            "userPassword/form API");
                }

            }else{
                csAssert.assertTrue(false,"Response of get password API is " + response);
            }

        }catch (Exception e){
            csAssert.assertTrue(false,"Response of get password API is " + response);
        }
        csAssert.assertAll();

    }






}
