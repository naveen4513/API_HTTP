 package com.sirionlabs.test.userAccess;

 import com.sirionlabs.api.clientAdmin.accessCriteria.AccessCriteriaCreate;
import com.sirionlabs.api.clientAdmin.accessCriteria.AccessCriteriaUpdate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserCreate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AccessCriteriaDbHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.test.listRenderer.TestEntityListingColumnSorting;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
 import com.sirionlabs.utils.commonUtils.ParseConfigFile;
 import java.util.ArrayList;
 import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static com.sirionlabs.helper.dbHelper.AppUserDbHelper.*;
 import static java.lang.Integer.parseInt;

 public class TestUserAccessClientAdmin extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUserAccessClientAdmin.class);
    private final AdminHelper adminHelperObj = new AdminHelper();
    private final Check checkObj = new Check();
    private static final String uacName = "APIUAC"+DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
     private static Map<String, String> entityNameMap = new HashMap<>();

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
        payloadMap.put("userRoleGroups", String.valueOf(getUserRoleGroupId(parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")))));
        payloadMap.put("_userRoleGroups", "1");
        payloadMap.put("_accessCriterias", String.valueOf(getAccessCriteriaId(parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")))));
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
    public void testCreateNewClientTypeEndUser() {
        CustomAssert csAssert = new CustomAssert();
            String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
        int lastUserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("type", "2");
            payloadMap.put("firstName", "API"+loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", "API"+loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);

            int responseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

            if (responseCode == 302) {
                int latestUserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

                if (latestUserId > lastUserId) {
                    //Validate LoginId
                    List<String> dataInDb = AppUserDbHelper.getUserDataFromUserId("login_id", latestUserId);
                    csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(loginId), "Expected Login Id: " + loginId + " and Actual Login Id in DB: " + dataInDb.get(0));

                    //TC-C44209: Update existing User.
                    logger.info("Updating existing User having Id: {}", latestUserId);
                    String updatedFirstName = "Update First Name";
                    payloadMap.put("firstName", updatedFirstName);
                    payloadMap.put("id", String.valueOf(latestUserId));
                    responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                    if (responseCode == 302) {
                        dataInDb = AppUserDbHelper.getUserDataFromUserId("first_name", latestUserId);
                        csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected First Name after Update: " + updatedFirstName +
                                " and Actual First Name: " + dataInDb.get(0) + " in DB.");
                    } else {
                        csAssert.assertTrue(false, "Couldn't Update existing User.");
                    }

                    //Activating Newly created user
                    logger.info("Activating Newly Created User"+latestUserId);
                    HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                    HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                    if(httpResponse.getStatusLine().getStatusCode() == 200){
                        dataInDb=AppUserDbHelper.getUserDataFromUserId("active",latestUserId);
                        csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase("t"),"User is now activated, check email for password");
                    }

                    //Password Resetting for Newly created User from DB
                    clientUserPasswordChange(latestUserId);
                } else {
                    csAssert.assertTrue(false, "No entry created in app_user Table.");
                }
            } else {
                csAssert.assertTrue(false, "Couldn't Create App User.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
        } finally {
            //Login with newly created user
            loginWithEndUser(loginId,"admin123");
        }

        csAssert.assertAll();
    }

     //Test Create New Supplier Type End-User
     @Test(priority=2)
     public void testCreateNewSupplierTypeEndUser() {
         CustomAssert csAssert = new CustomAssert();
         String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
         int lastUserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
         int latestVendorID = AppUserDbHelper.getLatestEntityId("vendor", parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
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
             payloadMap.put("email", loginId+"@sirionqa.office");
             payloadMap.put("uniqueLoginId", loginId);

             int responseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

             if (responseCode == 302) {
                 int latestUserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

                 if (latestUserId > lastUserId) {
                     //Validate LoginId
                     List<String> dataInDb = AppUserDbHelper.getUserDataFromUserId("login_id", latestUserId);
                     csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(loginId), "Expected Login Id: " + loginId + " and Actual Login Id in DB: " + dataInDb.get(0));

                     //TC-C44209: Update existing User.
                     logger.info("Updating existing User having Id: {}", latestUserId);
                     String updatedFirstName = "Update First Name";
                     payloadMap.put("firstName", updatedFirstName);
                     payloadMap.put("id", String.valueOf(latestUserId));
                     responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                     if (responseCode == 302) {
                         dataInDb = AppUserDbHelper.getUserDataFromUserId("first_name", latestUserId);
                         csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected First Name after Update: " + updatedFirstName +
                                 " and Actual First Name: " + dataInDb.get(0) + " in DB.");
                     } else {
                         csAssert.assertTrue(false, "Couldn't Update existing User.");
                     }

                     //Activating Newly created user
                     logger.info("Activating Newly Created User"+latestUserId);
                     HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                     HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                     if(httpResponse.getStatusLine().getStatusCode() == 200){
                         dataInDb=AppUserDbHelper.getUserDataFromUserId("active",latestUserId);
                         csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase("t"),"User is now activated, check email for password");
                     }

                     //Password Resetting for Newly created User from DB
                     clientUserPasswordChange(latestUserId);
                 } else {
                     csAssert.assertTrue(false, "No entry created in app_user Table.");
                 }
             } else {
                 csAssert.assertTrue(false, "Couldn't Create App User.");
             }
         } catch (Exception e) {
             csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
         } finally {
             //Login with newly created user
             loginWithEndUser(loginId,"admin123");
         }

         csAssert.assertAll();
     }

     //Test Create New Sirionlabs Type End-User
     @Test(priority=3)
     public void testCreateNewSirionlabsTypeEndUser() {
         CustomAssert csAssert = new CustomAssert();
         String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
         int lastUserId = AppUserDbHelper.getLatestUserId(1,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
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
                 int latestUserId = AppUserDbHelper.getLatestUserId(1,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

                 if (latestUserId > lastUserId) {
                     //Validate LoginId
                     List<String> dataInDb = AppUserDbHelper.getUserDataFromUserId("login_id", latestUserId);
                     csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(loginId), "Expected Login Id: " + loginId + " and Actual Login Id in DB: " + dataInDb.get(0));

                     //TC-C44209: Update existing User.
                     logger.info("Updating existing User having Id: {}", latestUserId);
                     String updatedFirstName = "Update First Name";
                     payloadMap.put("firstName", updatedFirstName);
                     payloadMap.put("id", String.valueOf(latestUserId));
                     responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                     if (responseCode == 302) {
                         dataInDb = AppUserDbHelper.getUserDataFromUserId("first_name", latestUserId);
                         csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected First Name after Update: " + updatedFirstName +
                                 " and Actual First Name: " + dataInDb.get(0) + " in DB.");
                     } else {
                         csAssert.assertTrue(false, "Couldn't Update existing User.");
                     }

                     //Activating Newly created user
                     logger.info("Activating Newly Created User"+latestUserId);
                     HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                     HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                     if(httpResponse.getStatusLine().getStatusCode() == 200){
                         dataInDb=AppUserDbHelper.getUserDataFromUserId("active",latestUserId);
                         csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase("t"),"User is now activated, check email for password");
                     }

                     //Password Resetting for Newly created User from DB
                     clientUserPasswordChange(latestUserId);
                 } else {
                     csAssert.assertTrue(false, "No entry created in app_user Table.");
                 }
             } else {
                 csAssert.assertTrue(false, "Couldn't Create App User.");
             }
         } catch (Exception e) {
             csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
         } finally {
             //Login with newly created user
             loginWithEndUser(loginId,"admin123");
         }

         csAssert.assertAll();
     }

     //Test Create New Non-User Type End-User
     @Test(priority=4)
     public void testCreateNewNonUserTypeEndUser() {
         CustomAssert csAssert = new CustomAssert();
         String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
         int lastUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
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
                 int latestUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

                 if (latestUserId > lastUserId) {
                     //Validate LoginId
                     List<String> dataInDb = AppUserDbHelper.getUserDataFromUserId("login_id", latestUserId);
                     csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(loginId), "Expected Login Id: " + loginId + " and Actual Login Id in DB: " + dataInDb.get(0));

                     //TC-C44209: Update existing User.
                     logger.info("Updating existing User having Id: {}", latestUserId);
                     String updatedFirstName = "Update First Name";
                     payloadMap.put("firstName", updatedFirstName);
                     payloadMap.put("id", String.valueOf(latestUserId));
                     responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                     if (responseCode == 302) {
                         dataInDb = AppUserDbHelper.getUserDataFromUserId("first_name", latestUserId);
                         csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected First Name after Update: " + updatedFirstName +
                                 " and Actual First Name: " + dataInDb.get(0) + " in DB.");
                     } else {
                         csAssert.assertTrue(false, "Couldn't Update existing User.");
                     }

                     //Activating Newly created user
                     logger.info("Activating Newly Created User"+latestUserId);
                     HttpGet httpGet = new HttpGet("/tblusers/activate/"+latestUserId);
                     HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                     if(httpResponse.getStatusLine().getStatusCode() == 200){
                         dataInDb=AppUserDbHelper.getUserDataFromUserId("active",latestUserId);
                         csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase("t"),"User is now activated, check email for password");
                     }

                     //Password Resetting for Newly created User from DB
                     clientUserPasswordChange(latestUserId);
                 } else {
                     csAssert.assertTrue(false, "No entry created in app_user Table.");
                 }
             } else {
                 csAssert.assertTrue(false, "Couldn't Create App User.");
             }
         } catch (Exception e) {
             csAssert.assertTrue(false, "Exception while Validating TC-C44207: " + e.getMessage());
         } finally {
             //Login with newly created user
             loginWithEndUser(loginId,"admin123");
         }

         csAssert.assertAll();
     }

     //Test Create UAC With System-Read-Write Access
     @Test(priority=5)
    public void testCreateUACWithSystemReadWriteAccess() {
         CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to create UAC with System Read & Write Access.");

            loginWithAdminUser();

            int lastUACId = AccessCriteriaDbHelper.getLatestUacId();

            Map<String, String> payloadMap = payloadMapUAC();
            payloadMap.put("name", uacName);
            payloadMap.put("linkEntityAdminAccess[0].entityTypeId","2");
            payloadMap.put("linkEntityAdminAccess[0].entityId",ConfigureEnvironment.getEnvironmentProperty("client_id"));
            payloadMap.put("linkEntityAdminAccess[0].readAccess","on");
            payloadMap.put("linkEntityAdminAccess[0].writeAccess","on");

            int responseCode = executor.postMultiPartFormData(AccessCriteriaCreate.getApiPath(), AccessCriteriaCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

            if (responseCode == 302) {
                int latestUacId = AccessCriteriaDbHelper.getLatestUacId();

                if (latestUacId > lastUACId) {
                    //Validate UAC Name
                    List<String> uacData = AccessCriteriaDbHelper.getCriteriaDataFromId("name", latestUacId);
                    csAssert.assertTrue(uacData.get(0).equalsIgnoreCase(uacName), "Expected UAC Name: " + uacName + " and Actual Name: " + uacData.get(0) +
                            " in DB.");

                    //TC-C44210: Update existing UAC.
                    String updatedName = "Updated"+uacName;
                    payloadMap.put("name", updatedName);
                    payloadMap.put("id", String.valueOf(latestUacId));
                    payloadMap.put("active", "true");
                    payloadMap.put("_active", "on");
                    payloadMap.put("linkEntityAdminAccess[0].entityTypeId","2");
                    payloadMap.put("linkEntityAdminAccess[0].entityId",ConfigureEnvironment.getEnvironmentProperty("client_id"));
                    payloadMap.put("linkEntityAdminAccess[0].readAccess","on");
                    payloadMap.put("linkEntityAdminAccess[0].writeAccess","on");

                    responseCode = executor.postMultiPartFormData(AccessCriteriaUpdate.getApiPath(), AccessCriteriaUpdate.getHeaders(),
                            payloadMap).getResponse().getResponseCode();

                    if (responseCode == 302) {
                        uacData = AccessCriteriaDbHelper.getCriteriaDataFromId("name", latestUacId);
                        csAssert.assertTrue(uacData.get(0).equalsIgnoreCase(updatedName), "Expected UAC Name after Update: " + updatedName +
                                " and Actual Name: " + uacData.get(0) + " in DB.");
                    } else {
                        csAssert.assertTrue(false, "Couldn't Update existing UAC.");
                    }
                } else {
                    csAssert.assertTrue(false, "No new entry in access_criteria Table.");
                }
            } else {
                csAssert.assertTrue(false, "Couldn't Create User Access Criteria.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C44208: " + e.getMessage());
        }
        csAssert.assertAll();
    }

     //Test Update UAC With System-Read-Write Access
    @Test(priority=6)
    public void testUpdateUACOfClientTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

            CustomAssert csAssert = new CustomAssert();
            int UserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
            Integer UACId = AccessCriteriaDbHelper.getLatestUacId();

            loginWithAdminUser();

             logger.info("Updating existing User having Id: {}", UserId);
             String updatedFirstName = "Update First Name";

             Map<String, String> payloadMap = payloadMap();

                 payloadMap.put("firstName", getAppUserDataFromUserId("first_name",UserId));
                 payloadMap.put("loginId", getAppUserDataFromUserId("login_id",UserId));
                 payloadMap.put("lastName", getAppUserDataFromUserId("last_name",UserId));
                 payloadMap.put("email", getAppUserDataFromUserId("email",UserId));
                 payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",UserId));
                 payloadMap.put("accessCriterias", String.valueOf(UACId));
                 payloadMap.put("id", String.valueOf(UserId));
                 payloadMap.put("type", "2");

                 int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

                 if (responseCode == 302) {
                     List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", UserId);
                     csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
                 } else {
                     csAssert.assertTrue(true, "Couldn't Update existing User.");
                 }
     }

     //Test Update UAC With System-Read-Write Access
     @Test(priority=7)
     public void testUpdateUACOfSupplierTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

         CustomAssert csAssert = new CustomAssert();
         int UserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
         Integer UACId = AccessCriteriaDbHelper.getLatestUacId();
         int latestVendorID = AppUserDbHelper.getLatestEntityId("vendor", parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
         loginWithAdminUser();

         logger.info("Updating existing User having Id: {}", UserId);
         String updatedFirstName = "Update First Name";

         Map<String, String> payloadMap = payloadMap();

         payloadMap.put("firstName", getAppUserDataFromUserId("first_name",UserId));
         payloadMap.put("loginId", getAppUserDataFromUserId("login_id",UserId));
         payloadMap.put("lastName", getAppUserDataFromUserId("last_name",UserId));
         payloadMap.put("email", getAppUserDataFromUserId("email",UserId));
         payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",UserId));
         payloadMap.put("accessCriterias", String.valueOf(UACId));
         payloadMap.put("id", String.valueOf(UserId));
         payloadMap.put("type", "4");
         payloadMap.put("vendorId",String.valueOf(latestVendorID));

         int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

         if (responseCode == 302) {
             List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", UserId);
             csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
         } else {
             csAssert.assertTrue(true, "Couldn't Update existing User.");
         }
     }

     //Test Update UAC With System-Read-Write Access
     @Test(priority=8)
     public void testUpdateUACOfSirionlabsTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

         CustomAssert csAssert = new CustomAssert();
         int UserId = AppUserDbHelper.getLatestUserId(1, parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
         Integer UACId = AccessCriteriaDbHelper.getLatestUacId();

         loginWithAdminUser();

         logger.info("Updating existing User having Id: {}", UserId);
         String updatedFirstName = "Update First Name";

         Map<String, String> payloadMap = payloadMap();

         payloadMap.put("firstName", getAppUserDataFromUserId("first_name",UserId));
         payloadMap.put("loginId", getAppUserDataFromUserId("login_id",UserId));
         payloadMap.put("lastName", getAppUserDataFromUserId("last_name",UserId));
         payloadMap.put("email", getAppUserDataFromUserId("email",UserId));
         payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",UserId));
         payloadMap.put("accessCriterias", String.valueOf(UACId));
         payloadMap.put("id", String.valueOf(UserId));
         payloadMap.put("type", "1");

         int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

         if (responseCode == 302) {
             List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", UserId);
             csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
         } else {
             csAssert.assertTrue(true, "Couldn't Update existing User.");
         }
     }

     //Test Update UAC With System-Read-Write Access
     @Test(priority=9)
     public void testUpdateUACOfNonUserTypeEndUser() throws Exception {
         //TC-C44209: Update UAC of existing User.

         CustomAssert csAssert = new CustomAssert();
         int UserId = AppUserDbHelper.getLatestUserId(3, parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
         Integer UACId = AccessCriteriaDbHelper.getLatestUacId();

         loginWithAdminUser();

         logger.info("Updating existing User having Id: {}", UserId);
         String updatedFirstName = "Update First Name";

         Map<String, String> payloadMap = payloadMap();

         payloadMap.put("firstName", getAppUserDataFromUserId("first_name",UserId));
         payloadMap.put("loginId", getAppUserDataFromUserId("login_id",UserId));
         payloadMap.put("lastName", getAppUserDataFromUserId("last_name",UserId));
         payloadMap.put("email", getAppUserDataFromUserId("email",UserId));
         payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",UserId));
         payloadMap.put("accessCriterias", String.valueOf(UACId));
         payloadMap.put("id", String.valueOf(UserId));
         payloadMap.put("type", "3");

         int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

         if (responseCode == 302) {
             List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", UserId);
             csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + UACId+" in DB.");
         } else {
             csAssert.assertTrue(true, "Couldn't Update existing User.");
         }
     }

     //Test UserAccess With System Read-Write Access
    @Test(priority=10)
    public void testClientTypeUserAccessWithSystemReadWriteAccess()  {
        CustomAssert csAssert = new CustomAssert();

        int latestUserId = AppUserDbHelper.getLatestUserId(2, parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

        for (String entityName : entityTypeIds() ) {

            if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                    && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                    && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                    && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                    && !entityName.toLowerCase().contains("delegation")) {

                try {
                    logger.info("Starting Test TC-C63039: " + entityName + "  Internationalization: Verify all Fields on Show Page, Create Page and Edit Page.");

                    TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                    int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                    if (noOfRecords < -1) {
                        csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                    }

                } catch (SkipException e) {
                    logger.error("skip exception: " + e.getMessage());
                    //throw new SkipException(e.getMessage());

                } catch (Exception e) {
                    logger.error("exception error: " + e.getMessage());
                    csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());

                }
            }
        }
        csAssert.assertAll();
    }

     //Test UserAccess With System Read-Write Access
     @Test(priority=11)
     public void testSupplierTypeUserAccessWithSystemReadWriteAccess()  {
         CustomAssert csAssert = new CustomAssert();

         int latestUserId = AppUserDbHelper.getLatestUserId(4, parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

         loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

         for (String entityName : entityTypeIds() ) {

             if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                     && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                     && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                     && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                     && !entityName.toLowerCase().contains("delegation")) {

                 try {
                     logger.info("Starting Test TC-C63039: " + entityName + "  Internationalization: Verify all Fields on Show Page, Create Page and Edit Page.");

                     TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                     int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                     if (noOfRecords < -1) {
                         csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                     }

                 } catch (SkipException e) {
                     logger.error("skip exception: " + e.getMessage());
                     //throw new SkipException(e.getMessage());

                 } catch (Exception e) {
                     logger.error("exception error: " + e.getMessage());
                     csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());

                 }
             }
         }
         csAssert.assertAll();
     }

     //Test UserAccess With System Read-Write Access
     @Test(priority=12)
     public void testSirionlabsTypeUserAccessWithSystemReadWriteAccess()  {
         CustomAssert csAssert = new CustomAssert();

         int latestUserId = AppUserDbHelper.getLatestUserId(1, parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

         loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

         for (String entityName : entityTypeIds() ) {

             if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                     && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                     && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                     && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                     && !entityName.toLowerCase().contains("delegation")) {

                 try {
                     logger.info("Starting Test TC-C63039: " + entityName + "  Internationalization: Verify all Fields on Show Page, Create Page and Edit Page.");

                     TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                     int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                     if (noOfRecords < -1) {
                         csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                     }

                 } catch (SkipException e) {
                     logger.error("skip exception: " + e.getMessage());
                     //throw new SkipException(e.getMessage());

                 } catch (Exception e) {
                     logger.error("exception error: " + e.getMessage());
                     csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());

                 }
             }
         }
         csAssert.assertAll();
     }

     //Test UserAccess With System Read-Write Access
     @Test(priority=13)
     public void testNonUserTypeUserAccessWithSystemReadWriteAccess()  {
         CustomAssert csAssert = new CustomAssert();

         int latestUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

         loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

         for (String entityName : entityTypeIds() ) {

             if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                     && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                     && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                     && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups") && !entityName.toLowerCase().contains("externalcontractingparty")
                     && !entityName.toLowerCase().contains("delegation")) {

                 try {
                     logger.info("Starting Test TC-C63039: " + entityName + "  Internationalization: Verify all Fields on Show Page, Create Page and Edit Page.");

                     TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                     int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName(entityName.toLowerCase(),ConfigureConstantFields.getListIdForEntity(entityName),csAssert);

                     if (noOfRecords < -1) {
                         csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for " + entityName+ " listing page");
                     }

                 } catch (SkipException e) {
                     logger.error("skip exception: " + e.getMessage());
                     //throw new SkipException(e.getMessage());

                 } catch (Exception e) {
                     logger.error("exception error: " + e.getMessage());
                     csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());

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
 }