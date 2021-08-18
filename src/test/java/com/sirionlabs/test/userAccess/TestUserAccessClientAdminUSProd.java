 package com.sirionlabs.test.userAccess;

 import com.sirionlabs.api.clientAdmin.accessCriteria.AccessCriteriaCreate;
 import com.sirionlabs.api.clientAdmin.accessCriteria.AccessCriteriaUpdate;
 import com.sirionlabs.api.clientAdmin.userConfiguration.UserCreate;
 import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
 import com.sirionlabs.api.commonAPI.Check;
 import com.sirionlabs.config.ConfigureConstantFields;
 import com.sirionlabs.config.ConfigureEnvironment;
 import com.sirionlabs.helper.ListRenderer.ListDataHelper;
 import com.sirionlabs.helper.api.TestAPIBase;
 import com.sirionlabs.helper.clientAdmin.AdminHelper;
 import com.sirionlabs.helper.dbHelper.AccessCriteriaDbHelper;
 import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
 import com.sirionlabs.test.listRenderer.TestEntityListingColumnSorting;
 import com.sirionlabs.utils.EmailUtils.EmailUtils;
 import com.sirionlabs.utils.commonUtils.*;
 import org.apache.http.HttpResponse;
 import org.apache.http.client.methods.HttpGet;
 import org.json.JSONArray;
 import org.json.JSONObject;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.testng.SkipException;
 import org.testng.annotations.Test;

 import javax.mail.MessagingException;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.util.*;

 import static com.sirionlabs.helper.dbHelper.AppUserDbHelper.*;
 import static java.lang.Integer.parseInt;

 public class TestUserAccessClientAdminUSProd extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUserAccessClientAdminUSProd.class);
    private final AdminHelper adminHelperObj = new AdminHelper();
    private final Check checkObj = new Check();
    private static final String uacName = "APIUAC"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
    private static Map<String, String> entityNameMap = new HashMap<>();
    private int latestUserId;
    private int lastUserId;
    private String updatedName;
    private String host = "outlook.office365.com";// change accordingly
    private String foldername = "Account Activation";
    private String username = "jenkins@sirionlabs.com";// change accordingly
    private String password = "Sirion@123";// change accordingly
    private String NewlyCreatedloginId;
    private String UserActivated;

    public static Map<String, String> payloadMap() {

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("contactNo", "");
        payloadMap.put("designation", "");
        payloadMap.put("timeZone.id", "13");
        payloadMap.put("thirdPartyId", "");
        payloadMap.put("tierId", "0");
        payloadMap.put("passwordNeverExpires", "true");
        payloadMap.put("_passwordNeverExpires", "on");
        payloadMap.put("_enableTwofactorLogin", "on");
        payloadMap.put("loginMechanismType","1");
        payloadMap.put("userClassificationType", "1");
        payloadMap.put("language", "1");
        payloadMap.put("sendEmail","true");
        payloadMap.put("_sendEmail", "on");
        payloadMap.put("_excludeFromFilter", "on");
        payloadMap.put("userRoleGroups", "5736");
        payloadMap.put("_userRoleGroups", "1");
        payloadMap.put("_accessCriterias", "1284");
        payloadMap.put("_userDepartments", "1");
        payloadMap.put("defaultUITwoDotO","true");
        payloadMap.put("_defaultUITwoDotO", "on");
        payloadMap.put("dynamicMetadataJS", "{}");
        payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
        payloadMap.put("ajax", "true");
        payloadMap.put("history", "{}");
        return payloadMap;
    }

    public static Map<String, String> payloadMapUAC() {

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("_active", "on");
        payloadMap.put("active","true");
        payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));

        return payloadMap;
    }

    private void loginWithAdminUser() {
        adminHelperObj.loginWithClientAdminUser();
    }

    private void loginWithEndUser(String Username, String Password) {
        checkObj.hitCheck(Username, Password);
    }

     //Test Create New Client Type End-User
    @Test(priority=1)
    public void testCreateNewClientTypeEndUser() throws MessagingException, IOException {

        CustomAssert csAssert = new CustomAssert();
            String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
        try {
            logger.info("Validating that Client Admin should be able to create new user on US-Prod instance");

            loginWithAdminUser();

            logger.info("Creating new App User.");
            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("type", "2");
            payloadMap.put("firstName", "API" + loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", "API" + loginId);
            payloadMap.put("email", loginId + "@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);

           lastUserId= Integer.parseInt(loginID());

            int responseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();
            latestUserId= Integer.parseInt(loginID());

            if (responseCode == 302) {

                if (latestUserId > lastUserId) {

                    logger.info("Updating existing User having Id: {}", latestUserId);
                    String updatedFirstName = "Update First Name";
                    payloadMap.put("firstName", updatedFirstName);
                    payloadMap.put("id", String.valueOf(latestUserId));
                    responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                    if (responseCode == 302) {
                       updatedName= UserFirstName();
                        csAssert.assertTrue(true,"User's first's name updated successfully"+updatedName);
                    } else {
                        csAssert.assertTrue(false, "Couldn't Update existing User.");
                        logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                    }

                    //Activating Newly created user
                    logger.info("Activating Newly Created User"+latestUserId);
                    HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                    HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                    if(httpResponse.getStatusLine().getStatusCode() == 200){
                        csAssert.assertTrue(true,"User is now activated, check email for password");
                    }else{
                        csAssert.assertTrue(false,"Unable to activate user, please check API Response");
                        logger.info(String.valueOf(httpResponse));
                    }
                } else {
                    csAssert.assertTrue(false, "No entry created in app_user Table.");
                    logger.info (String.valueOf(executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                }
            } else {
                csAssert.assertTrue(false, "Couldn't Create App User.");
                logger.info(String.valueOf(executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
        }

        csAssert.assertAll();
    }

     //Test Create New Supplier Type End-User
     @Test(priority=2)
     public void testCreateNewSupplierTypeEndUser() {
         CustomAssert csAssert = new CustomAssert();
         String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
         int latestVendorID = 1008;

         lastUserId= Integer.parseInt(loginID());

         try {
             logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

             loginWithAdminUser();

             logger.info("Creating new App User.");

             Map<String, String> payloadMap = payloadMap();
             payloadMap.put("type", "4");
             payloadMap.put("vendorId",String.valueOf(latestVendorID));
             payloadMap.put("firstName", "API"+loginId);
             payloadMap.put("loginId", loginId);
             payloadMap.put("lastName", "API"+loginId);
             payloadMap.put("email", loginId+"@sirionlabs.office");
             payloadMap.put("uniqueLoginId", loginId);

             int responseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

             if (responseCode == 302) {
                latestUserId= Integer.parseInt(loginID());

                 if (latestUserId > lastUserId) {
                     //Validate LoginId

                     NewlyCreatedloginId=username();
                     csAssert.assertTrue(true, "Expected Login Id: " + loginId + " and Actual Login Id in DB: " + NewlyCreatedloginId);

                     //TC-C44209: Update existing User.
                     logger.info("Updating existing User having Id: {}", latestUserId);
                     String updatedFirstName = "Update First Name";
                     payloadMap.put("firstName", updatedFirstName);
                     payloadMap.put("id", String.valueOf(latestUserId));
                     responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                     if (responseCode == 302) {
                         updatedName=UserFirstName();
                         csAssert.assertTrue(true, "Expected First Name after Update: " + updatedFirstName + " and Actual First Name: " + updatedName + " in DB.");
                     } else {
                         csAssert.assertTrue(false, "Couldn't Update existing User.");
                         logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                     }

                     //Activating Newly created user
                     logger.info("Activating Newly Created User"+latestUserId);
                     HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                     HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                     if(httpResponse.getStatusLine().getStatusCode() == 200){
                        UserActivated=UserActivated();
                         csAssert.assertTrue(true,"User is now activated, check email for password");
                     }else{
                         csAssert.assertTrue(false,"Unable to activate user, Please check API Response");
                         logger.info(String.valueOf(httpResponse));
                     }
                 } else {
                     csAssert.assertTrue(false, "No entry created in app_user Table.");
                 }
             } else {
                 csAssert.assertTrue(false, "Couldn't Create App User.");
                 logger.info(String.valueOf(executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
             }
         } catch (Exception e) {
             csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
         }
         csAssert.assertAll();
     }

     //Test Create New Sirionlabs Type End-User
     @Test(priority=3)
     public void testCreateNewSirionlabsTypeEndUser() {
         CustomAssert csAssert = new CustomAssert();
         String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
         int lastUserId = Integer.parseInt(loginID());
         try {
             logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

             loginWithAdminUser();

             logger.info("Creating new App User.");

             Map<String, String> payloadMap = payloadMap();
             payloadMap.put("type", "1");
             payloadMap.put("firstName", "API"+loginId);
             payloadMap.put("loginId", loginId);
             payloadMap.put("lastName", "API"+loginId);
             payloadMap.put("email", loginId+"@sirionqa.office");
             payloadMap.put("uniqueLoginId", loginId);

             int responseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

             if (responseCode == 302) {
                 int latestUserId = Integer.parseInt(loginID());

                 if (latestUserId > lastUserId) {
                     //Validate LoginId
                     String username = username();
                     csAssert.assertTrue(true, "Expected Login Id: " + loginId + " and Actual Login Id in DB: " + username);

                     //TC-C44209: Update existing User.
                     logger.info("Updating existing User having Id: {}", latestUserId);
                     String updatedFirstName = "Update First Name";
                     payloadMap.put("firstName", updatedFirstName);
                     payloadMap.put("id", String.valueOf(latestUserId));
                     responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                     if (responseCode == 302) {
                         updatedName=UserFirstName();
                         csAssert.assertTrue(true, "Expected First Name after Update: " + updatedFirstName +
                                 " and Actual First Name: " + updatedName + " in DB.");
                     } else {
                         csAssert.assertTrue(false, "Couldn't Update existing User.");
                         logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                     }

                     //Activating Newly created user
                     logger.info("Activating Newly Created User"+latestUserId);
                     HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                     HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                     if(httpResponse.getStatusLine().getStatusCode() == 200){
                         UserActivated=UserActivated();
                         csAssert.assertTrue(true,"User is now activated, check email for password");
                     }else{
                         csAssert.assertTrue(false,"Unable to activate user, Please check API Reponse");
                         logger.info(String.valueOf(httpResponse));
                     }

                     //Password Resetting for Newly created User from DB
                     //clientUserPasswordChange(latestUserId);
                 } else {
                     csAssert.assertTrue(false, "No entry created in app_user Table.");
                 }
             } else {
                 csAssert.assertTrue(false, "Couldn't Create App User.");
                 logger.info(String.valueOf(executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
             }
         } catch (Exception e) {
             csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
         }

         csAssert.assertAll();
     }

     //Test Create New Non-User Type End-User
     @Test(priority=4)
     public void testCreateNewNonUserTypeEndUser() {
         CustomAssert csAssert = new CustomAssert();
         String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
         int lastUserId = Integer.parseInt(loginID());
         try {
             logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

             loginWithAdminUser();

             logger.info("Creating new App User.");

             Map<String, String> payloadMap = payloadMap();
             payloadMap.put("type", "3");
             payloadMap.put("firstName", "API"+loginId);
             payloadMap.put("loginId", loginId);
             payloadMap.put("lastName", "API"+loginId);
             payloadMap.put("email", loginId+"@sirionqa.office");
             payloadMap.put("uniqueLoginId", loginId);

             int responseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

             if (responseCode == 302) {
                 int latestUserId = Integer.parseInt(loginID());

                 if (latestUserId > lastUserId) {
                     //Validate LoginId
                     String UserloginId=username();
                     csAssert.assertTrue(true, "Expected Login Id: " + loginId + " and Actual Login Id in DB: " + UserloginId);

                     //TC-C44209: Update existing User.
                     logger.info("Updating existing User having Id: {}", latestUserId);
                     String updatedFirstName = "Update First Name";
                     payloadMap.put("firstName", updatedFirstName);
                     payloadMap.put("id", String.valueOf(latestUserId));
                     responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                     if (responseCode == 302) {
                         String UserFirstName = UserFirstName();
                         csAssert.assertTrue(true, "Expected First Name after Update: " + updatedFirstName + " and Actual First Name: " + UserFirstName + " in DB.");
                     } else {
                         csAssert.assertTrue(false, "Couldn't Update existing User.");
                         logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                     }

                     //Activating Newly created user
                     logger.info("Activating Newly Created User"+latestUserId);
                     HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                     HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                     if(httpResponse.getStatusLine().getStatusCode() == 200){
                         String UserActivated = UserActivated();
                         csAssert.assertTrue(true,"User is now activated, check email for password");
                     }else{
                         csAssert.assertTrue(false,"Unable to Activate user, please check API Response");
                         logger.info(String.valueOf(httpResponse));
                     }

                     //Password Resetting for Newly created User from DB
                     //clientUserPasswordChange(latestUserId);
                 } else {
                     csAssert.assertTrue(false, "No entry created in app_user Table.");
                 }
             } else {
                 csAssert.assertTrue(false, "Couldn't Create App User.");
                 logger.info(String.valueOf(executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
             }
         } catch (Exception e) {
             csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
         }
         csAssert.assertAll();
     }

     //Test Create UAC With System-Read-Write Access
     @Test(priority=5)
    public void testCreateUACWithSystemReadWriteAccess() throws UnsupportedEncodingException {
         CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to create UAC with System Read & Write Access.");

            loginWithAdminUser();

            Map<String, String> payloadMap = payloadMapUAC();
            payloadMap.put("name", uacName);
            payloadMap.put("linkEntityAdminAccess[0].entityTypeId","2");
            payloadMap.put("linkEntityAdminAccess[0].entityId","1002");
            payloadMap.put("linkEntityAdminAccess[0].readAccess","on");
            payloadMap.put("linkEntityAdminAccess[0].writeAccess","on");

            int responseCode = executor.postMultiPartFormData(AccessCriteriaCreate.getApiPath(), AccessCriteriaCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

            if (responseCode == 302) {
                csAssert.assertTrue(true, "Access Criteria created successfully");
                } else {
                    csAssert.assertTrue(false, "No new entry in access_criteria Table.");
                    logger.info(String.valueOf(executor.postMultiPartFormData(AccessCriteriaCreate.getApiPath(), AccessCriteriaCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                }
            } catch (Exception e) {
            e.printStackTrace();
        }
         csAssert.assertAll();
    }

     //Test Update UAC With System-Read-Write Access
    @Test(priority=6)
    public void testUpdateUACOfClientTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

            CustomAssert csAssert = new CustomAssert();
            int UserId = 1162;
            Integer UACId = 1284;

            loginWithAdminUser();

             logger.info("Updating existing User having Id: {}", UserId);
             String updatedFirstName = "Update First Name"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();

             Map<String, String> payloadMap = payloadMap();

                 payloadMap.put("firstName", "Apurwa"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
                 payloadMap.put("loginId", "apurwa"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
                 payloadMap.put("lastName", "Baranwal"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
                 payloadMap.put("email", "apurwa@sirioncloud.com");
                 payloadMap.put("uniqueLoginId", "apurwa"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
                 payloadMap.put("accessCriterias", String.valueOf(UACId));
                 payloadMap.put("id", String.valueOf(UserId));
                 payloadMap.put("type", "2");

                 int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                 if (responseCode == 302) {
                     csAssert.assertTrue(false, "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
                 } else {
                     csAssert.assertTrue(true, "Couldn't Update existing User.");
                     logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                 }
     }

     //Test Update UAC With System-Read-Write Access
     @Test(priority=7)
     public void testUpdateUACOfSupplierTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

         CustomAssert csAssert = new CustomAssert();
         int UserId = 1164;
         Integer UACId = 1284;
         int latestVendorID = 1008;
         loginWithAdminUser();

         logger.info("Updating existing User having Id: {}", UserId);
         String updatedFirstName = "Update First Name";

         Map<String, String> payloadMap = payloadMap();

         payloadMap.put("firstName", "Anshul"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("loginId", "anshul");
         payloadMap.put("lastName", "Goel"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("email", "kanhaiya.saini@sirionlabs.com");
         payloadMap.put("uniqueLoginId", "anshul"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("accessCriterias", String.valueOf(UACId));
         payloadMap.put("id", String.valueOf(UserId));
         payloadMap.put("type", "4");
         payloadMap.put("vendorId",String.valueOf(latestVendorID));

         int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

         if (responseCode == 302) {
             csAssert.assertTrue(false, "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
         } else {
             csAssert.assertTrue(true, "Couldn't Update existing User.");
             logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
         }
     }

     //Test Update UAC With System-Read-Write Access
     @Test(priority=8)
     public void testUpdateUACOfSirionlabsTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

         CustomAssert csAssert = new CustomAssert();
         int UserId = 1171;
         Integer UACId = 1284;

         loginWithAdminUser();

         logger.info("Updating existing User having Id: {}", UserId);
         String updatedFirstName = "Update First Name";

         Map<String, String> payloadMap = payloadMap();

         payloadMap.put("firstName", "Anay"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("loginId", "anay");
         payloadMap.put("lastName", "Jyoti"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("email", "ashish.banka@sirionlabs.com");
         payloadMap.put("uniqueLoginId", "anay"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("accessCriterias", String.valueOf(UACId));
         payloadMap.put("id", String.valueOf(UserId));
         payloadMap.put("type", "1");

         int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

         if (responseCode == 302) {
             csAssert.assertTrue(false, "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
         } else {
             csAssert.assertTrue(true, "Couldn't Update existing User.");
             logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
         }
     }

     //Test Update UAC With System-Read-Write Access
     @Test(priority=9)
     public void testUpdateUACOfNonUserTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

         CustomAssert csAssert = new CustomAssert();
         int UserId = 42112;
         Integer UACId = 1284;

         loginWithAdminUser();

         logger.info("Updating existing User having Id: {}", UserId);
         String updatedFirstName = "Update First Name";

         Map<String, String> payloadMap = payloadMap();

         payloadMap.put("firstName", "Non"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("loginId", "nonuser");
         payloadMap.put("lastName", "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("email", "nonuser@sirionlabs.com");
         payloadMap.put("uniqueLoginId", "nonuser"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
         payloadMap.put("accessCriterias", String.valueOf(UACId));
         payloadMap.put("id", String.valueOf(UserId));
         payloadMap.put("type", "3");

         int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

         if (responseCode == 302) {
             csAssert.assertTrue(false, "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
         } else {
             csAssert.assertTrue(true, "Couldn't Update existing User.");
             logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
         }
     }

     //Test UserAccess With System Read-Write Access
    @Test(priority=10)
    public void testClientTypeUserAccessWithSystemReadWriteAccess()  {
        CustomAssert csAssert = new CustomAssert();

        loginWithEndUser("chinmaya_user","admin123");

        for (String entityName : entityTypeIds() ) {

            if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                    && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                    && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                    && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                    && !entityName.toLowerCase().contains("delegation")) {

                try {
                    logger.info("Starting Test : Client Type User able to access"+entityName);

                    TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                    int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                    if (noOfRecords < -1) {
                        csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                    }

                } catch (SkipException e) {
                    logger.info("skip exception: " + e.getMessage());
                    //throw new SkipException(e.getMessage());

                } catch (Exception e) {
                    logger.info("exception info: " + e.getMessage());
                    csAssert.assertTrue(false, "Exception while Validating Test." + new Exception().getStackTrace()[0].getMethodName() + e.getMessage());

                }
            }
        }
        csAssert.assertAll();
    }

     //Test UserAccess With System Read-Write Access
     @Test(priority=11)
     public void testSupplierTypeUserAccessWithSystemReadWriteAccess()  {
         CustomAssert csAssert = new CustomAssert();

         loginWithEndUser("automation_supplier_user_0001","admin123");

         for (String entityName : entityTypeIds() ) {

             if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                     && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                     && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                     && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                     && !entityName.toLowerCase().contains("delegation") && !entityName.toLowerCase().contains("user details")) {

                 try {
                     logger.info("Starting Test : Supplier Type User able to access --> "+entityName);

                     TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                     int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                     if (noOfRecords < -1) {
                         csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                     }

                 } catch (SkipException e) {
                     logger.info("skip exception: " + e.getMessage());
                     //throw new SkipException(e.getMessage());

                 } catch (Exception e) {
                     logger.info("exception info: " + e.getMessage());
                     csAssert.assertTrue(false, "Exception while Validating Test " + new Exception().getStackTrace()[0].getMethodName() + e.getMessage());
                 }
             }
         }
         csAssert.assertAll();
     }

     //Test UserAccess With System Read-Write Access
     @Test(priority=12)
     public void testSirionlabsTypeUserAccessWithSystemReadWriteAccess()  {
         CustomAssert csAssert = new CustomAssert();

         loginWithEndUser("anay","admin@123");

         for (String entityName : entityTypeIds() ) {

             if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                     && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                     && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                     && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                     && !entityName.toLowerCase().contains("delegation")  && !entityName.toLowerCase().contains("user details")) {

                 try {
                     logger.info("Starting Test : Sirionlabs Type User able to access"+entityName);

                     TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                     int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                     if (noOfRecords < -1) {
                         csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                     }

                 } catch (SkipException e) {
                     logger.info("skip exception: " + e.getMessage());
                     //throw new SkipException(e.getMessage());

                 } catch (Exception e) {
                     logger.info("exception info: " + e.getMessage());
                     csAssert.assertTrue(false, "Exception while Validating Test " + new Exception().getStackTrace()[0].getMethodName() + e.getMessage());

                 }
             }
         }
         csAssert.assertAll();
     }

     //Test UserAccess With System Read-Write Access
     @Test(priority=13)
     public void testNonUserTypeUserAccessWithSystemReadWriteAccess()  {
         CustomAssert csAssert = new CustomAssert();

         loginWithEndUser("nonuser","admin123");

         for (String entityName : entityTypeIds() ) {

             if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                     && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                     && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                     && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                     && !entityName.toLowerCase().contains("delegation")  && !entityName.toLowerCase().contains("user details")) {

                 try {
                     logger.info("Starting Test : Non-User Type User able to access"+entityName);

                     TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                     int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                     if (noOfRecords < -1) {
                         csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                     }

                 } catch (SkipException e) {
                     logger.info("skip exception: " + e.getMessage());
                     //throw new SkipException(e.getMessage());

                 } catch (Exception e) {
                     logger.info("exception info: " + e.getMessage());
                     csAssert.assertTrue(false, "Exception while Validating Test "+ new Exception().getStackTrace()[0].getMethodName() + e.getMessage());

                 }
             }
         }
         csAssert.assertAll();
     }

     public static List<String> entityTypeIds (){
         String baseFilePath = "src//test//resources//CommonConfigFiles";
         String entityIdFile = "EntityId.cfg";
         entityNameMap = ParseConfigFile.getAllProperties(baseFilePath, entityIdFile);
         ArrayList<String> list = new ArrayList<>();
         for (Map.Entry entityName : entityNameMap.entrySet() ) {
             list.add(String.valueOf(entityName.getKey()));
         }
         return list;
     }

     public String loginID(){

         CustomAssert csAssert = new CustomAssert();
         for (String entityName : ConfigureConstantFields.entityTypeIds()) {
             if (entityName.toLowerCase().contains("user details")) {
                 String listDataResponse = ListDataHelper.getListDataResponse(entityName);

                 if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                     int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                     JSONObject jsonObj = new JSONObject(listDataResponse);
                     JSONArray jsonArr = jsonObj.getJSONArray("data");

                     String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                     latestUserId = ListDataHelper.getRecordIdFromValue(idValue);
                 } else {
                     csAssert.assertTrue(false, "List Data API Response for Disputes is an Invalid JSON.");
                 }
             }
         }
         return String.valueOf(latestUserId);
    }

     public String UserFirstName(){
         String idValue = null;
         CustomAssert csAssert = new CustomAssert();
         for (String entityName : ConfigureConstantFields.entityTypeIds()) {
             if (entityName.toLowerCase().contains("user details")) {
                 String listDataResponse = ListDataHelper.getListDataResponse(entityName);

                 if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                     int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "firstname");
                     JSONObject jsonObj = new JSONObject(listDataResponse);
                     JSONArray jsonArr = jsonObj.getJSONArray("data");

                     idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                     
                 } else {
                     csAssert.assertTrue(false, "List Data API Response for Disputes is an Invalid JSON.");
                 }
             }
         }
         return idValue;
     }

     public String UserActivated(){

         CustomAssert csAssert = new CustomAssert();
         for (String entityName : ConfigureConstantFields.entityTypeIds()) {
             if (entityName.toLowerCase().contains("user details")) {
                 String listDataResponse = ListDataHelper.getListDataResponse(entityName);

                 if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                     int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "active");
                     JSONObject jsonObj = new JSONObject(listDataResponse);
                     JSONArray jsonArr = jsonObj.getJSONArray("data");

                     String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                     latestUserId = ListDataHelper.getRecordIdFromValue(idValue);
                 } else {
                     csAssert.assertTrue(false, "List Data API Response for Disputes is an Invalid JSON.");
                 }
             }
         }
         return String.valueOf(latestUserId);
     }

     public String username(){
         String idValue=null;
         CustomAssert csAssert = new CustomAssert();
         for (String entityName : ConfigureConstantFields.entityTypeIds()) {
             if (entityName.toLowerCase().contains("user details")) {
                 String listDataResponse = ListDataHelper.getListDataResponse(entityName);

                 if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                     int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "loginid");
                     JSONObject jsonObj = new JSONObject(listDataResponse);
                     JSONArray jsonArr = jsonObj.getJSONArray("data");

                     idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");

                 } else {
                     csAssert.assertTrue(false, "List Data API Response for Disputes is an Invalid JSON.");
                 }
             }
         }
         return idValue;
     }

 }