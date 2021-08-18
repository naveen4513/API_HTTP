package com.sirionlabs.test.enduseremail;

import com.sirionlabs.api.clientAdmin.userConfiguration.UserResetPassword;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.PasswordUpdate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.test.TestPasswordFlow.LoginUtility;
import com.sirionlabs.utils.commonUtils.CustomAssert;

import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;
import java.security.GeneralSecurityException;


public class TestForcePasswordReset extends TestAPIBase {

    private int userId;
    private String loginId;
    private String publicKey;
    private String newpassword;


    private Check checkObj = new Check();
    private AdminHelper adminHelperObj = new AdminHelper();
    private PasswordUpdate forceReset = new PasswordUpdate();
    private LoginUtility loginUtil = new LoginUtility();

    private final static Logger logger = LoggerFactory.getLogger(TestForcePasswordReset.class);


    @BeforeClass
    public  void before(){
        String passwordResetConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestForcePasswordResetConfigFilePath");
        String passwordResetConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestForcePasswordResetConfigFileName");
        userId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(passwordResetConfigFilePath, passwordResetConfigFileName,"userid"));
        loginId = ParseConfigFile.getValueFromConfigFile(passwordResetConfigFilePath, passwordResetConfigFileName,"loginid");
        publicKey = ParseConfigFile.getValueFromConfigFile(passwordResetConfigFilePath, passwordResetConfigFileName,"rsapublickey");
        newpassword = ParseConfigFile.getValueFromConfigFile(passwordResetConfigFilePath, passwordResetConfigFileName,"newpassword");
        newpassword  = newpassword + ((int) (Math.random() * (100 - 10)) + 10);
    }

    @Test(description = "C153044")
    public void ResetPasswordFromAdmin() throws GeneralSecurityException, IOException {
        CustomAssert csAssert = new CustomAssert();
        loginUtil.deleteEntryFromTable(loginId + "@sirionqa.office","Your password has been reset");
        adminHelperObj.loginWithClientAdminUser();
        int statuscode = UserResetPassword.hitResetPasswordAPI(userId);
        if(statuscode==200) {
           String authToken = loginUtil.getPasswordEmailTokenfromEmail(loginId + "@sirionqa.office",
                   "Your password has been reset",csAssert);
          logger.info("Auth Token is : "+ authToken);
          System.out.println("newPassword is  "+ newpassword);
           String payload =  forceReset.getPayload(newpassword,publicKey);
            APIValidator response = forceReset.postUpdatePassword(payload, authToken);
          if(response.getResponse().getResponseCode()!=200 ||
                  !(JSONUtility.parseJson(response.getResponse().getResponseBody(),"$.message"))
                          .equals("Password updated successfully.")){
              csAssert.assertTrue(false,"password not reset successfully");
          }

          if(!checkObj.hitCheckforceReset(loginId,newpassword)){
              csAssert.assertTrue(false,"user not able to login from updated password");
          }
        }
            csAssert.assertAll();
    }

    @Test(description = "C153046")
    public void InvalidApiPath() throws GeneralSecurityException, IOException {
        CustomAssert csAssert = new CustomAssert();
        loginUtil.deleteEntryFromTable(loginId + "@sirionqa.office","Your password has been reset");
        adminHelperObj.loginWithClientAdminUser();
        int statuscode = UserResetPassword.hitResetPasswordAPI(userId);
        if(statuscode==200) {
            String authToken = loginUtil.getPasswordEmailTokenfromEmail(loginId + "@sirionqa.office",
                    "Your password has been reset",csAssert);
            logger.info("Auth Token is : "+ authToken);
            System.out.println("newPassword is  "+ newpassword);
            String payload =  forceReset.getPayload(newpassword,publicKey);
            APIValidator response = forceReset.postUpdatePasswordInvalidPath(payload, authToken);
            if(response.getResponse().getResponseCode()!=401 ||
                    !(JSONUtility.parseJson(response.getResponse().getResponseBody(),"$.errorMessage"))
                            .equals("Signature did not match with last authentication.")){
                csAssert.assertTrue(false,"Message is not correct in case of invalid path" +
                        response.getResponse().getResponseBody());
            }

        }
        csAssert.assertAll();
    }

    @Test(description = "C153047")
    public void InvalidApiPayload() throws GeneralSecurityException, IOException {
        CustomAssert csAssert = new CustomAssert();
        loginUtil.deleteEntryFromTable(loginId + "@sirionqa.office","Your password has been reset");
        adminHelperObj.loginWithClientAdminUser();
        int statuscode = UserResetPassword.hitResetPasswordAPI(userId);
        if(statuscode==200) {
            String authToken = loginUtil.getPasswordEmailTokenfromEmail(loginId + "@sirionqa.office",
                    "Your password has been reset",csAssert);
            logger.info("Auth Token is : "+ authToken);
            System.out.println("newPassword is  "+ newpassword);
            String payload =  forceReset.getPayload(newpassword,publicKey);
            payload = payload.replace("Password","Passworddd");
            APIValidator response = forceReset.postUpdatePassword(payload, authToken);
            if(response.getResponse().getResponseCode()!=200 ||
                    !(JSONUtility.parseJson(response.getResponse().getResponseBody(),
                            "$.header.response.status")).equals("applicationError")){
                csAssert.assertTrue(false,"Message is not correct in case of invalid payload" +
                        response.getResponse().getResponseBody());
            }

        }
        csAssert.assertAll();
    }

   @Test(description = "C153050")
    public void PasswordPolicyMismatch() throws GeneralSecurityException, IOException {
        CustomAssert csAssert = new CustomAssert();
        loginUtil.deleteEntryFromTable(loginId + "@sirionqa.office","Your password has been reset");
        adminHelperObj.loginWithClientAdminUser();
        int statuscode = UserResetPassword.hitResetPasswordAPI(userId);
        if(statuscode==200) {
            String authToken = loginUtil.getPasswordEmailTokenfromEmail(loginId + "@sirionqa.office",
                    "Your password has been reset",csAssert);
            logger.info("Auth Token is : "+ authToken);
             String invalidPassword = "123";
            String payload =  forceReset.getPayload(invalidPassword,publicKey);
            APIValidator response = forceReset.postUpdatePassword(payload, authToken);
            if(response.getResponse().getResponseCode()!=200 ||
                    !(JSONUtility.parseJson(response.getResponse().getResponseBody(),
                            "$.errors[0].defaultMessage"))
                            .equals("Password must be 8 to  16 characters long. Password must mix alpha and numeric characters.")){
                csAssert.assertTrue(false,"Message is not correct in case of password doesnt follow " +
                        "password policy" +
                        response.getResponse().getResponseBody());
            }

        }
        csAssert.assertAll();
    }

    @Test(description = "C153045")
    public void InvalidApiMethod() throws GeneralSecurityException, IOException {
        CustomAssert csAssert = new CustomAssert();
        loginUtil.deleteEntryFromTable(loginId + "@sirionqa.office","Your password has been reset");
        adminHelperObj.loginWithClientAdminUser();
        int statuscode = UserResetPassword.hitResetPasswordAPI(userId);
        if(statuscode==200) {
            String authToken = loginUtil.getPasswordEmailTokenfromEmail(loginId + "@sirionqa.office",
                    "Your password has been reset",csAssert);
            logger.info("Auth Token is : "+ authToken);
            System.out.println("newPassword is  "+ newpassword);
            String payload =  forceReset.getPayload(newpassword,publicKey);
            APIValidator response = forceReset.updatePasswordInvalidMethod(payload, authToken);
            if(response.getResponse().getResponseCode()!=405 ||
                    !(response.getResponse().getResponseBody().equals(""))){
                csAssert.assertTrue(false,"Message is not correct in case of invalid method" +
                        response.getResponse().getResponseBody());
            }

        }
        csAssert.assertAll();
    }


    @AfterClass
    public void afterClass(){
        checkObj.hitCheck( ConfigureEnvironment.getEnvironmentProperty("j_username"),ConfigureEnvironment.getEnvironmentProperty("password"));

    }

    }
