package com.sirionlabs.test.enduseremail;

import com.sirionlabs.api.clientAdmin.userConfiguration.UserUnlock;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


public class TestIncorrectLoginnLockedemail extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestIncorrectLoginnLockedemail.class);

    private static String configFilePath;
    private static String configFileName;
    private static String endUserName;
    private static String endUseremail;
    private static String endUserFullName;
    private static String endUsereId;
    private static Integer invalidAttempt;


    Check check = new Check();
    AdminHelper helper = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TFAConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TFAConfigFileName");
        invalidAttempt = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,
                configFileName, "invalidattemptcount"));
    }


    @AfterClass
    public void afterClass() {
        new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    @DataProvider()
    public Object[][] dataProviderForLockandIncorrectLogin() {
        List<Object[]> allTestData = new ArrayList<>();



        String[] flows = {"default","incorrect login attempt client admin"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForLockandIncorrectLogin")
    public void TestIncorrectLoginLockedemail(String flow) {
        CustomAssert csAssert = new CustomAssert();

        endUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"username");
        endUseremail =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"email");
        endUserFullName =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"fullname");
        endUsereId =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"userid");

        // validate End user and client-admin

        for (int i = 0; i <invalidAttempt ; i++) {
            deleteIncorrectLoginLockedemail(endUseremail);
            int statuscode = check.hitCheckwithInvalidCredentials(endUserName,"xyz");
            if(statuscode == 401){
                validateIncorrectLoginEmail(endUseremail,endUserFullName,endUserName,csAssert);

            }else{
                csAssert.assertTrue(false,"check api expected status 401 but actual "
                        + statuscode+"with invalid credentials");
            }
        }

        validateLockedEmail(endUseremail,endUserName,csAssert);
        deleteIncorrectLoginLockedemail(endUseremail);
        check.hitCheckwithInvalidCredentials(endUserName,"xyz");
        validateIncorrectLoginEmailAfterLock(endUseremail,csAssert);
        helper.loginWithClientAdminUser();
        unlockUser(csAssert);

                 csAssert.assertAll();

    }


    @Test(dataProvider = "dataProviderForLockandIncorrectLogin")
    public void TestSendEmailFalse(String flow) {
        CustomAssert csAssert = new CustomAssert();

        endUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"username");
        endUseremail =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"email");
        endUserFullName =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"fullname");
        endUsereId =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                flow,"userid");

        // validate End user and client-admin

        for (int i = 0; i <invalidAttempt ; i++) {
            deleteIncorrectLoginLockedemail(endUseremail);
            markSendEmailFlagDB(false,endUsereId);
            int statuscode = check.hitCheckwithInvalidCredentials(endUserName,"xyz");
            if(statuscode == 401){
                validateIncorrectLoginEmailsendFlagFalse(endUseremail,csAssert);

            }else{
                csAssert.assertTrue(false,"check api expected status 401 but actual "
                        + statuscode+"with invalid credentials");
            }
        }

        validateLockedEmailsendEmailFalse(endUseremail,csAssert);
        helper.loginWithClientAdminUser();
        unlockUser(csAssert);


        csAssert.assertAll();
        markSendEmailFlagDB(true,endUsereId);
    }




    private void unlockUser(CustomAssert csAssert){
        int unlockStatusCode = UserUnlock.hitUsersUnlockAPI(Integer.parseInt(endUsereId));
        if(unlockStatusCode==200){
            logger.info(endUserName+" is unlocked");

        }else{
            csAssert.assertTrue(false,endUserName+" is not unlocked");
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
                                " Please reset your password or contact the administrator. Your login ID is "+endUserName+".")
                        , "mail body is not correct");

            } else {
                csAssert.assertTrue(false, "LockedUser email not triggered for email -> " + email);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting email from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }


    private void validateIncorrectLoginEmail(String email, String fullName, String loginId, CustomAssert csAssert){
        String select_query = "select * from system_emails where to_mail = '"+email+"' and subject ilike '%Incorrect login attempt%'";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(select_query);

            if (results.size() != 0) {
                String subject = results.get(0).get(3);
                String body = results.get(0).get(4);

                csAssert.assertEquals(subject,
                        "Incorrect login attempt",
                        "subject is not correct --> " + subject);

                Document html = Jsoup.parse(body);
                body = html.getElementsByTag("body").get(0).getElementsByTag("tr").get(10)
                        .getElementsByTag("p").text();
                csAssert.assertTrue(body.contains("If this was not you, please reset your password here to secure your account")
                        , "mail body is not correct");
                csAssert.assertTrue(body.contains("Hi "+fullName+", There was a failed login attempt for your username. " +
                                "Usually this just means you mistyped or forgot your password. Login ID : "+loginId+" Failed on : ")
                        , "mail body is not correct");

            } else {
                csAssert.assertTrue(false, "Incorrect login attempt email not triggered for email -> " + email);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting email from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }



    private void validateIncorrectLoginEmailsendFlagFalse(String email, CustomAssert csAssert){
        String select_query = "select * from system_emails where to_mail = '"+email+"' and subject ilike '%Incorrect login attempt%'";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(select_query);

            if (results.size() != 0) {
                csAssert.assertFalse(true,
                        "Incorrect Login Email even if send email is false");
                  }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting email from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }

    private void validateLockedEmailsendEmailFalse(String email, CustomAssert csAssert){
        String select_query = "select * from system_emails where to_mail = '"+email+"' and subject ilike '%Your Sirion account has been locked%'";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(select_query);

            if (results.size() != 0) {
                csAssert.assertTrue(false,
                        "LockedUser email triggered even if send email is false -> " + email);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting email from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }


    private void validateIncorrectLoginEmailAfterLock(String email, CustomAssert csAssert){
        String select_query = "select * from system_emails where to_mail = '"+email+"' and subject ilike '%Incorrect login attempt%'";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(select_query);

            if (results.size() != 0) {
                csAssert.assertFalse(true,"Incorrect Login Email triggered even after user locked");
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting email from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }




    private void deleteIncorrectLoginLockedemail(String email){

        String delete_query = "delete from system_emails where to_mail =  '"+email+"' and " +
                "(subject ilike '%Incorrect login attempt%' or " +
                "subject ilike '%Your Sirion account has been locked%')";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            sqlObj.deleteDBEntry(delete_query);
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while delete Data from system_emails from DB using query [{}]. {}", delete_query, e.getMessage());
        }
    }

    private boolean markSendEmailFlagDB(Boolean flag ,String userId){
        boolean results=false;
        String update_query = "update app_user set send_email = "+flag+" where id = "+userId+";";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            results =  sqlObj.updateDBEntry(update_query);
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while updating data in DB using query [{}]. {}", update_query, e.getMessage());
        }
        return results;
    }









    }
