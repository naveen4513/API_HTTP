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
 import org.apache.http.HttpResponse;
 import org.apache.http.client.methods.HttpGet;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.testng.SkipException;
 import org.testng.annotations.Test;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 import static com.sirionlabs.helper.dbHelper.AppUserDbHelper.*;
 import static java.lang.Integer.parseInt;

 public class TestUserCreationClientTypeUser extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUserCreationClientTypeUser.class);
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
        payloadMap.put("_userRoleGroups", "1070");
        payloadMap.put("_accessCriterias", "1");
        payloadMap.put("accessCriterias", "1004");
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
        for (int i = 1; i<=1800;i++) {
            CustomAssert csAssert = new CustomAssert();
            String loginId = "User" + DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
            int lastUserId = AppUserDbHelper.getLatestUserId(2, Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
            try {
                logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

                loginWithAdminUser();

                logger.info("Creating new App User.");

                Map<String, String> payloadMap = payloadMap();
                payloadMap.put("type", "2");
                payloadMap.put("firstName", "API" + loginId);
                payloadMap.put("loginId", loginId);
                payloadMap.put("lastName", "API" + loginId);
                payloadMap.put("email", loginId + "@sirionqa.office");
                payloadMap.put("uniqueLoginId", loginId);

                int responseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

                if (responseCode == 302) {
                    int latestUserId = AppUserDbHelper.getLatestUserId(2, Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

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
                        logger.info("Activating Newly Created User" + latestUserId);
                        HttpGet httpGet = new HttpGet("/tblusers/activate/" + latestUserId);
                        HttpResponse httpResponse = APIUtils.getRequest(httpGet);

                        if (httpResponse.getStatusLine().getStatusCode() == 200) {
                            dataInDb = AppUserDbHelper.getUserDataFromUserId("active", latestUserId);
                            csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase("t"), "User is now activated, check email for password");
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
                loginWithEndUser(loginId, "admin123");
            }

            csAssert.assertAll();
        }
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