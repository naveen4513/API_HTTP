package com.sirionlabs.test.userAccess;

import com.sirionlabs.api.clientAdmin.accessCriteria.AccessCriteriaCreate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserCreate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AccessCriteriaDbHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.test.listRenderer.TestEntityListingColumnSorting;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static com.sirionlabs.helper.dbHelper.AppUserDbHelper.*;

public class TestUserSupplierAccessCriteria extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUserSupplierAccessCriteria.class);
    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();


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
        payloadMap.put("vendorId", "");
        payloadMap.put("loginMechanismType","1");
        payloadMap.put("userClassificationType", "1");
        payloadMap.put("language", "1");
        payloadMap.put("sendEmail","true");
        payloadMap.put("_sendEmail", "on");
        payloadMap.put("_excludeFromFilter", "on");
        payloadMap.put("userRoleGroups", String.valueOf(getUserRoleGroupId(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")))));
        payloadMap.put("_userRoleGroups", "1");
        payloadMap.put("_accessCriterias", "1");
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

    //Login with Client Admin
    private void loginWithAdminUser() {
        adminHelperObj.loginWithClientAdminUser();
    }

    //Login with End-User
    private void loginWithEndUser(String Username, String Password) {
        checkObj.hitCheck(Username, Password);
    }


    //Test Create UAC With Supplier Read-Write Access
    @Test(priority=1)
    public void testCreateUACWithSupplierReadWriteAccess() {
        CustomAssert csAssert = new CustomAssert();
        String uacName = "APISupplierUAC"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();

        try {
            logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to create UAC with Specific Contract Read & Write Access.");

            loginWithAdminUser();
            Map<String, String> payloadMap = payloadMapUAC();
            int latestVHID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
            int latestSupplierID = AppUserDbHelper.getLatestSupplierId("relation",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")),latestVHID);
            int lastUACId = AccessCriteriaDbHelper.getLatestUacId();
            payloadMap.put("name", uacName);
            payloadMap.put("supplierIds",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[5].entityTypeId","1");
            payloadMap.put("linkEntityAdminAccess[5].entityId",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[5].readAccess","on");
            payloadMap.put("linkEntityAdminAccess[5].writeAccess","on");


            int responseCode = executor.postMultiPartFormData(AccessCriteriaCreate.getApiPath(), AccessCriteriaCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

            if (responseCode == 302) {
                int latestUacId = AccessCriteriaDbHelper.getLatestUacId();

                if (latestUacId > lastUACId) {
                    //Validate UAC Name
                    List<String> uacData = AccessCriteriaDbHelper.getCriteriaDataFromId("name", latestUacId);
                    csAssert.assertTrue(uacData.get(0).equalsIgnoreCase(uacName), "Expected UAC Name: " + uacName + " and Actual Name: " + uacData.get(0) +
                            " in DB.");
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

/*-------------------------------------------------------------------------------------------Client Type User Test-cases------------------------------------------------------------------------------------------*/

    //Test Update User Access Criteria for End-User
    @Test(priority=2)
    public void testUpdateClientTypeUserWithUACOfSupplierReadWriteAccess() throws Exception {
        //TC-C44209: Update UAC of existing User.

        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacIdFromName(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        loginWithAdminUser();

        logger.info("Updating existing User having Id: {}", latestUserId);
        String updatedFirstName = "Update First Name";

        Map<String, String> payloadMap = payloadMap();

        payloadMap.put("firstName", getAppUserDataFromUserId("first_name",latestUserId));
        payloadMap.put("loginId", getAppUserDataFromUserId("login_id",latestUserId));
        payloadMap.put("lastName", getAppUserDataFromUserId("last_name",latestUserId));
        payloadMap.put("email", getAppUserDataFromUserId("email",latestUserId));
        payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",latestUserId));
        payloadMap.put("accessCriterias", String.valueOf(latestUACId));
        payloadMap.put("id", String.valueOf(latestUserId));
        payloadMap.put("type", "2");

        int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

        if (responseCode == 302) {
            List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", latestUserId);
            csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + String.valueOf(latestUACId)+" in DB.");
        } else {
            csAssert.assertTrue(false, "Couldn't Update existing User.");
        }

        while(true){
            Thread.sleep(10000);
            String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,latestUACId);
            if(Integer.parseInt(value)==3){
                Thread.sleep(10000);
                AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,latestUACId);
                break;
            }
        }
    }


    //Test Supplier Access Read-Write Access for Client Type User
    @Test(priority=3)
    public void testClientTypeUserAccessSupplierReadWrite() throws Exception {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to create UAC with Specific Contract Read & Write Access.");

        int latestUserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacId();
        int supplierId = AccessCriteriaDbHelper.getContarctAccessId(latestUACId,182);

        loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

        try {
            logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Supplier on listing page");

            TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
            int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
            String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

            if (noOfRecords < -1) {
                csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
            }
            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                if(supplierId==recordId){
                    csAssert.assertTrue(true,"User has access to Contract" + recordId +" and "+ supplierId);
                }else{
                    csAssert.assertFalse(true,"User does not have access to Contract" + recordId +" and "+ supplierId);
                }
            }

        } catch (SkipException e) {
            logger.error("skip exception: " + e.getMessage());
            //throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("exception error: " + e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    //Test Create New End-User with Supplier Access to a user via Client Type user profile page
    @Test(priority=4)
    public void testCreateNewClientTypeUserSupplierAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int latestVHID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int lastUserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
        int latestSupplierID = AppUserDbHelper.getLatestSupplierId("relation",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")),latestVHID);

        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user with Supplier access from profile page.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", "API"+loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", "Test"+loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("supplierIds",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].entityTypeId","1");
            payloadMap.put("linkEntityAdminAccess[1].entityId",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].readAccess","on");
            payloadMap.put("linkEntityAdminAccess[1].writeAccess","on");
            payloadMap.put("type", "2");
            payloadMap.remove("");



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
                    while(true){
                        Thread.sleep(60000);
                        String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,1);
                        if(Integer.parseInt(value)==3){
                            AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,1);
                            break;
                        }
                    }
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
            try {
                logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Vendor on listing page");

                TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
                String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

                if (noOfRecords < -1) {
                    csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
                }

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if(latestSupplierID==recordId){
                        csAssert.assertTrue(true,"User has access to Supplier" + recordId +" and "+ latestSupplierID);
                    }else{
                        csAssert.assertFalse(true,"User does not have access to Supplier" + recordId +" and "+ latestSupplierID);
                    }
                }

            } catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
            }
        }
    }


/*-------------------------------------------------------------------------------------------Supplier Type User Test-cases------------------------------------------------------------------------------------------*/


    //Test Update User Access Criteria for End-User
    @Test(priority=5)
    public void testUpdateSupplierTypeUserWithUACOfSupplierReadWriteAccess() throws Exception {
        //TC-C44209: Update UAC of existing User.

        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacIdFromName(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestVendorID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        loginWithAdminUser();

        logger.info("Updating existing User having Id: {}", latestUserId);
        String updatedFirstName = "Update First Name";

        Map<String, String> payloadMap = payloadMap();

        payloadMap.put("firstName", getAppUserDataFromUserId("first_name",latestUserId));
        payloadMap.put("loginId", getAppUserDataFromUserId("login_id",latestUserId));
        payloadMap.put("lastName", getAppUserDataFromUserId("last_name",latestUserId));
        payloadMap.put("email", getAppUserDataFromUserId("email",latestUserId));
        payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",latestUserId));
        payloadMap.put("accessCriterias", String.valueOf(latestUACId));
        payloadMap.put("id", String.valueOf(latestUserId));
        payloadMap.put("type", "4");
        payloadMap.put("vendorId",String.valueOf(latestVendorID));

        int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

        if (responseCode == 302) {
            List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", latestUserId);
            csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + String.valueOf(latestUACId)+" in DB.");
        } else {
            csAssert.assertTrue(false, "Couldn't Update existing User.");
        }

        while(true){
            Thread.sleep(10000);
            String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,latestUACId);
            if(Integer.parseInt(value)==3){
                Thread.sleep(10000);
                AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,latestUACId);
                break;
            }
        }
    }


    //Test Supplier Access Read-Write Access for Supplier Type User
    @Test(priority=6)
    public void testSupplierTypeUserAccessSupplierReadWrite() throws Exception {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to create UAC with Specific Contract Read & Write Access.");

        int latestUserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacIdFromName(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int supplierId = AccessCriteriaDbHelper.getContarctAccessId(latestUACId,182);

        loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

        try {
            logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Supplier on listing page");

            TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
            int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
            String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

            if (noOfRecords < -1) {
                csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
            }
            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                if(supplierId==recordId){
                    csAssert.assertTrue(true,"User has access to Contract" + recordId +" and "+ supplierId);
                }else{
                    csAssert.assertFalse(true,"User does not have access to Contract" + recordId +" and "+ supplierId);
                }
            }

        } catch (SkipException e) {
            logger.error("skip exception: " + e.getMessage());
            //throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("exception error: " + e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    //Test Create New End-User with Supplier Access to a user via Client Type user profile page
    @Test(priority=7)
    public void testCreateNewSupplierTypeUserSupplierAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int latestVHID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int lastUserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
        int latestSupplierID = AppUserDbHelper.getLatestSupplierId("relation",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")),latestVHID);
        int latestVendorID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user with Supplier access from profile page.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", "API"+loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", "Test"+loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("supplierIds",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].entityTypeId","1");
            payloadMap.put("linkEntityAdminAccess[1].entityId",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].readAccess","on");
            payloadMap.put("linkEntityAdminAccess[1].writeAccess","on");
            payloadMap.put("type", "4");
            payloadMap.remove("");
            payloadMap.put("vendorId",String.valueOf(latestVendorID));

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
                    while(true){
                        Thread.sleep(60000);
                        String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,1);
                        if(Integer.parseInt(value)==3){
                            AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,1);
                            break;
                        }
                    }
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
            try {
                logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Vendor on listing page");

                TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
                String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

                if (noOfRecords < -1) {
                    csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
                }

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if(latestSupplierID==recordId){
                        csAssert.assertTrue(true,"User has access to Supplier" + recordId +" and "+ latestSupplierID);
                    }else{
                        csAssert.assertFalse(true,"User does not have access to Supplier" + recordId +" and "+ latestSupplierID);
                    }
                }

            } catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
            }
        }
    }


/*-------------------------------------------------------------------------------------------Sirionlabs Type User Test-cases------------------------------------------------------------------------------------------*/


    //Test Update User Access Criteria for End-User
    @Test(priority=8)
    public void testUpdateSirionlabsTypeUserWithUACOfSupplierReadWriteAccess() throws Exception {
        //TC-C44209: Update UAC of existing User.

        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(1,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacIdFromName(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        loginWithAdminUser();

        logger.info("Updating existing User having Id: {}", latestUserId);
        String updatedFirstName = "Update First Name";

        Map<String, String> payloadMap = payloadMap();

        payloadMap.put("firstName", getAppUserDataFromUserId("first_name",latestUserId));
        payloadMap.put("loginId", getAppUserDataFromUserId("login_id",latestUserId));
        payloadMap.put("lastName", getAppUserDataFromUserId("last_name",latestUserId));
        payloadMap.put("email", getAppUserDataFromUserId("email",latestUserId));
        payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",latestUserId));
        payloadMap.put("accessCriterias", String.valueOf(latestUACId));
        payloadMap.put("id", String.valueOf(latestUserId));
        payloadMap.put("type", "1");

        int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

        if (responseCode == 302) {
            List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", latestUserId);
            csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + String.valueOf(latestUACId)+" in DB.");
        } else {
            csAssert.assertTrue(false, "Couldn't Update existing User.");
        }

        while(true){
            Thread.sleep(10000);
            String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,latestUACId);
            if(Integer.parseInt(value)==3){
                Thread.sleep(10000);
                AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,latestUACId);
                break;
            }
        }
    }


    //Test Supplier Access Read-Write Access for Supplier Type User
    @Test(priority=9)
    public void testSirionlabsTypeUserAccessSupplierReadWrite() throws Exception {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to create UAC with Specific Contract Read & Write Access.");

        int latestUserId = AppUserDbHelper.getLatestUserId(1,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacIdFromName(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int supplierId = AccessCriteriaDbHelper.getContarctAccessId(latestUACId,182);

        loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

        try {
            logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Supplier on listing page");

            TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
            int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
            String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

            if (noOfRecords < -1) {
                csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
            }
            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                if(supplierId==recordId){
                    csAssert.assertTrue(true,"User has access to Contract" + recordId +" and "+ supplierId);
                }else{
                    csAssert.assertFalse(true,"User does not have access to Contract" + recordId +" and "+ supplierId);
                }
            }

        } catch (SkipException e) {
            logger.error("skip exception: " + e.getMessage());
            //throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("exception error: " + e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    //Test Create New End-User with Supplier Access to a user via Client Type user profile page
    @Test(priority=10)
    public void testCreateNewSirionlabsTypeUserSupplierAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int latestVHID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int lastUserId = AppUserDbHelper.getLatestUserId(1,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
        int latestSupplierID = AppUserDbHelper.getLatestSupplierId("relation",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")),latestVHID);

        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user with Supplier access from profile page.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", "API"+loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", "Test"+loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("supplierIds",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].entityTypeId","1");
            payloadMap.put("linkEntityAdminAccess[1].entityId",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].readAccess","on");
            payloadMap.put("linkEntityAdminAccess[1].writeAccess","on");
            payloadMap.put("type", "1");
            payloadMap.remove("");

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
                    while(true){
                        Thread.sleep(60000);
                        String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,1);
                        if(Integer.parseInt(value)==3){
                            AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,1);
                            break;
                        }
                    }
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

            try {
                logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Vendor on listing page");

                TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
                String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

                if (noOfRecords < -1) {
                    csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
                }

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if(latestSupplierID==recordId){
                        csAssert.assertTrue(true,"User has access to Supplier" + recordId +" and "+ latestSupplierID);
                    }else{
                        csAssert.assertFalse(true,"User does not have access to Supplier" + recordId +" and "+ latestSupplierID);
                    }
                }

            } catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
            }
        }
    }


/*-------------------------------------------------------------------------------------------Non-User Type User Test-cases------------------------------------------------------------------------------------------*/


    //Test Update User Access Criteria for End-User
    @Test(priority=11)
    public void testUpdateNonUserTypeUserWithUACOfSupplierReadWriteAccess() throws Exception {
        //TC-C44209: Update UAC of existing User.

        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacIdFromName(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        loginWithAdminUser();

        logger.info("Updating existing User having Id: {}", latestUserId);
        String updatedFirstName = "Update First Name";

        Map<String, String> payloadMap = payloadMap();

        payloadMap.put("firstName", getAppUserDataFromUserId("first_name",latestUserId));
        payloadMap.put("loginId", getAppUserDataFromUserId("login_id",latestUserId));
        payloadMap.put("lastName", getAppUserDataFromUserId("last_name",latestUserId));
        payloadMap.put("email", getAppUserDataFromUserId("email",latestUserId));
        payloadMap.put("uniqueLoginId", getAppUserDataFromUserId("unique_login_id",latestUserId));
        payloadMap.put("accessCriterias", String.valueOf(latestUACId));
        payloadMap.put("id", String.valueOf(latestUserId));
        payloadMap.put("type", "3");

        int responseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

        if (responseCode == 302) {
            List<String> dataInDb = AppUserDbHelper.getUserAccessCriteriaDataUserId("access_criteria_id", latestUserId);
            csAssert.assertTrue(dataInDb.get(0).equalsIgnoreCase(updatedFirstName), "Expected Access Criteria ID/Data after Update: " + String.valueOf(latestUACId)+" in DB.");
        } else {
            csAssert.assertTrue(false, "Couldn't Update existing User.");
        }

        while(true){
            Thread.sleep(10000);
            String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,latestUACId);
            if(Integer.parseInt(value)==3){
                Thread.sleep(10000);
                AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,latestUACId);
                break;
            }
        }
    }


    //Test Supplier Access Read-Write Access for Supplier Type User
    @Test(priority=12)
    public void testNonUserTypeUserAccessSupplierReadWrite() throws Exception {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to create UAC with Specific Contract Read & Write Access.");

        int latestUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestUACId = AccessCriteriaDbHelper.getLatestUacIdFromName(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int supplierId = AccessCriteriaDbHelper.getContarctAccessId(latestUACId,182);

        loginWithEndUser(getAppUserDataFromUserId("login_id",latestUserId),"admin123");

        try {
            logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Supplier on listing page");

            TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
            int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
            String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

            if (noOfRecords < -1) {
                csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
            }
            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                if(supplierId==recordId){
                    csAssert.assertTrue(true,"User has access to Contract" + recordId +" and "+ supplierId);
                }else{
                    csAssert.assertFalse(true,"User does not have access to Contract" + recordId +" and "+ supplierId);
                }
            }

        } catch (SkipException e) {
            logger.error("skip exception: " + e.getMessage());
            //throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("exception error: " + e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    //Test Create New End-User with Supplier Access to a user via Client Type user profile page
    @Test(priority=13)
    public void testCreateNewNonUserTypeUserSupplierAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int latestVHID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int lastUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
        int latestSupplierID = AppUserDbHelper.getLatestSupplierId("relation",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")),latestVHID);

        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user with Supplier access from profile page.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", "API"+loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", "Test"+loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("supplierIds",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].entityTypeId","1");
            payloadMap.put("linkEntityAdminAccess[1].entityId",String.valueOf(latestSupplierID));
            payloadMap.put("linkEntityAdminAccess[1].readAccess","on");
            payloadMap.put("linkEntityAdminAccess[1].writeAccess","on");
            payloadMap.put("type", "3");
            payloadMap.remove("");

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

                    while(true){
                        Thread.sleep(60000);
                        String value = AppUserDbHelper.accessCriteriaComputationStatus(latestUserId,1);
                        if(Integer.parseInt(value)==3){
                            AccessCriteriaDbHelper.deleteAccessCriteriaComputation(latestUserId,1);
                            break;
                        }
                    }
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

            try {
                logger.info("Starting Test TC-C63039: Verfiying that Logged in user should able to see Vendor on listing page");

                TestEntityListingColumnSorting noOfRecordsObj = new TestEntityListingColumnSorting();
                int noOfRecords = noOfRecordsObj.getTotalRecordsCountForEntityName("suppliers",3,csAssert);
                String listDataResponse = ListDataHelper.getListDataResponse("suppliers");

                if (noOfRecords < -1) {
                    csAssert.assertFalse(false,"There is no data in listing page, either data not exist or Access-Criteria test got failed for Supplier listing page");
                }

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if(latestSupplierID==recordId){
                        csAssert.assertTrue(true,"User has access to Supplier" + recordId +" and "+ latestSupplierID);
                    }else{
                        csAssert.assertFalse(true,"User does not have access to Supplier" + recordId +" and "+ latestSupplierID);
                    }
                }

            } catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
            }
        }
    }


    /*@Test(priority =14)
    public void deleteAllData() throws SQLException {

        int lastUserId = AppUserDbHelper.getLatestUserId(2);
        int lastUACId = AccessCriteriaDbHelper.getLatestUacId();

        AccessCriteriaDbHelper.deleteUserDetails(lastUserId);
        AccessCriteriaDbHelper.deleteAccessCriteria(lastUACId);
    }*/

}