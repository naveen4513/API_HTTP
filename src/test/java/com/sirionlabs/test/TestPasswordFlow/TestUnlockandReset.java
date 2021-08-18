package com.sirionlabs.test.TestPasswordFlow;

import com.sirionlabs.api.clientAdmin.userConfiguration.UserUnlock;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

public class TestUnlockandReset extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUnlockandReset.class);
    private static String configFilePath;
    private static String configFileName;
    private int clientId;
    private int invalidAttempt;
    private String endUserLoginId;
    private String endUserLoginEmail;
    private int endUserId;
    private  String resetLink;

    Check check = new Check();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestPasswordFlowFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestPasswordFlowFileName");
        invalidAttempt = Integer.parseInt(ParseConfigFile.getValueFromConfigFile
                (configFilePath, configFileName,"invalidattempt"));

        endUserLoginId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "enduser","sirionloginid");

        clientId =    new AdminHelper().getClientIdFromDB();

        List<String> endUserData = AppUserDbHelper.getUserDataFromUserLoginId
                ("id,email", endUserLoginId, clientId);

        endUserId = Integer.parseInt(endUserData.get(0));
        endUserLoginEmail = endUserData.get(1);
    }

    @Test
    public void Test(){
        CustomAssert csAssert = new CustomAssert();

        // validate End user and client-admin
        for (int i = 0; i <invalidAttempt ; i++) {

            int statuscode = check.hitCheckwithInvalidCredentials(endUserLoginId,"xyz");
            if(statuscode != 401) {
                csAssert.assertTrue(false, "check api expected status 401 but actual "
                        + statuscode + "with invalid credentials");
            }
        }

        validateLockedEmail(endUserLoginEmail,endUserLoginId,csAssert);
        unlockUser(endUserId,csAssert);
        validateUNLockedEmail(endUserLoginEmail,endUserLoginId,csAssert);
        resetPasswordResetLink();



    }

    private void resetPasswordResetLink(){
        executor.get(resetLink.split("//")[1], ApiHeaders.getDefaultLegacyHeaders(),false);

    }

    private void unlockUser(int endUserId, CustomAssert csAssert){
        int unlockStatusCode = UserUnlock.hitUsersUnlockAPI(endUserId);
        if(unlockStatusCode==200){
            logger.info(endUserLoginId+" is unlocked");

        }else{
            csAssert.assertTrue(false,endUserLoginId+" is not unlocked");
        }
    }

    private void validateUNLockedEmail(String email, String loginId, CustomAssert csAssert){
        String select_query = "select * from system_emails where to_mail = '"+email+"' and subject ilike '%Your Sirion account has been unlocked%'";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(select_query);

            if (results.size() != 0) {
                String subject = results.get(0).get(3);
                String body = results.get(0).get(4);
                csAssert.assertEquals(subject,
                        "Your Sirion account has been locked",
                        "subject is not correct --> " + subject);

                Document html = Jsoup.parse(body);
                body = html.getElementsByTag("body").get(0).getElementsByTag("tr").get(9)
                        .getElementsByTag("p").text();
                csAssert.assertTrue(body.contains("Your Sirion account has been unlocked. " +
                                "Your login ID is "+endUserLoginId+". To reset your login password, " +
                                "complete the process by clicking on the link. " +
                                "This link will expire within next 1 hours. " +
                                "Note: This link is applicable for one time usage only.")
                        ,"Message in body is incorrect" );

               resetLink =  html.getElementsByTag("body").get(0).getElementsByTag("tr").get(9)
                        .getElementsByTag("a").get(0).attr("href");

            } else {
                csAssert.assertTrue(false, "UnLock User email not triggered for email -> " + email);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting email from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }

    private void validateLockedEmail(String email, String loginId, CustomAssert csAssert){
        String select_query = "select * from system_emails where to_mail = '"+email+"' and subject ilike '%Your Sirion account has been locked%'";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(select_query);

            if (results.size() != 0) {
                String subject = results.get(0).get(3);
                String body = results.get(0).get(4);
                csAssert.assertEquals(subject,
                        "Your Sirion account has been locked",
                        "subject is not correct --> " + subject);

                Document html = Jsoup.parse(body);
                body = html.getElementsByTag("body").get(0).getElementsByTag("tr").get(9)
                        .getElementsByTag("p").text();
                csAssert.assertTrue(body.contains("Your Sirion account has been locked due to maximum number of failed attempts." +
                                " Please reset your password or contact the administrator. Your login ID is "+endUserLoginId+".")
                        , "mail body is not correct");

            } else {
                csAssert.assertTrue(false, "LockedUser email not triggered for email -> " + email);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting email from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }




}
