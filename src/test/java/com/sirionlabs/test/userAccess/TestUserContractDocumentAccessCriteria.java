package com.sirionlabs.test.userAccess;

import com.sirionlabs.api.clientAdmin.userConfiguration.UserCreate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AccessCriteriaDbHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static com.sirionlabs.helper.dbHelper.AppUserDbHelper.clientUserPasswordChange;
import static com.sirionlabs.helper.dbHelper.AppUserDbHelper.getUserRoleGroupId;
import static com.sirionlabs.utils.commonUtils.DateUtils.getCurrentDateInDDMMYYYYHHMMSS;

public class TestUserContractDocumentAccessCriteria extends APIUtils{

    private final static Logger logger = LoggerFactory.getLogger(TestUserContractDocumentAccessCriteria.class);
    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();
    String listDataJsonStr = null;
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
    private void loginWithAdminUser() {
        adminHelperObj.loginWithClientAdminUser();
    }
    private void loginWithEndUser(String Username, String Password) {
        checkObj.hitCheck(Username, Password);
    }

    //Test Create New End-User with Contract Document Access to a user via user profile page
    @Test(priority=1)
    public void testCreateNewClientTypeEndUserContractDocumentAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int lastUserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestContractDocumentID = AppUserDbHelper.getLatestContractDocumentId(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ getCurrentDateInDDMMYYYYHHMMSS();

        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("documentIds",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].id",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].clientActive","on");
            payloadMap.put("type", "2");

            TestAPIBase testAPIBase = new TestAPIBase();
            int responseCode = testAPIBase.executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

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
                    responseCode = testAPIBase.executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

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

    //Test Create New End-User with Contract Document Access to a user via user profile page
    @Test(priority=2)
    public void testCreateNewSupplierTypeEndUserContractDocumentAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int lastUserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestContractDocumentID = AppUserDbHelper.getLatestContractDocumentId(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ getCurrentDateInDDMMYYYYHHMMSS();
        int latestVendorID = AppUserDbHelper.getLatestEntityId("vendor",Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("documentIds",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].id",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].clientActive","on");
            payloadMap.put("type", "4");
            payloadMap.put("vendorId",String.valueOf(latestVendorID));

            TestAPIBase testAPIBase = new TestAPIBase();
            int responseCode = testAPIBase.executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

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
                    responseCode = testAPIBase.executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

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

    //Test Create New End-User with Contract Document Access to a user via user profile page
    @Test(priority=3)
    public void testCreateNewSirionlabsTypeEndUserContractDocumentAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int lastUserId = AppUserDbHelper.getLatestUserId(1,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestContractDocumentID = AppUserDbHelper.getLatestContractDocumentId(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ getCurrentDateInDDMMYYYYHHMMSS();

        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("documentIds",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].id",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].clientActive","on");
            payloadMap.put("type", "1");

            TestAPIBase testAPIBase = new TestAPIBase();
            int responseCode = testAPIBase.executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

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
                    responseCode = testAPIBase.executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

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

    //Test Create New End-User with Contract Document Access to a user via user profile page
    @Test(priority=4)
    public void testCreateNewNonUserTypeEndUserContractDocumentAccessProfilePage() {
        CustomAssert csAssert = new CustomAssert();
        int lastUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        int latestContractDocumentID = AppUserDbHelper.getLatestContractDocumentId(Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));
        String loginId = "User"+ getCurrentDateInDDMMYYYYHHMMSS();

        try {
            logger.info("Starting Test TC-C44207: Validating that Client Admin should be able to create new user.");

            loginWithAdminUser();

            logger.info("Creating new App User.");

            Map<String, String> payloadMap = payloadMap();
            payloadMap.put("firstName", loginId);
            payloadMap.put("loginId", loginId);
            payloadMap.put("lastName", loginId);
            payloadMap.put("email", loginId+"@sirionqa.office");
            payloadMap.put("uniqueLoginId", loginId);
            payloadMap.put("documentIds",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].id",String.valueOf(latestContractDocumentID));
            payloadMap.put("selectedDocuments[0].clientActive","on");
            payloadMap.put("type", "3");

            TestAPIBase testAPIBase = new TestAPIBase();
            int responseCode = testAPIBase.executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

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
                    responseCode = testAPIBase.executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

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

    //Test End-User with Contract Document Access to a user via user profile page
    @Test(priority=5)
    public void testContractDocumentAccessWithReadWriteAccessForClientTypeUser() throws Exception {
        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(2,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        String contractIds = AccessCriteriaDbHelper.getContarctDocumentAccessId(latestUserId);

        String contractId = contractIds.replace("{","");
        String contractId1 = contractId.replace("}","");
        String[] contractDocumentID =contractId1.split(",");

        loginWithEndUser(AppUserDbHelper.getUserLoginId(latestUserId),"admin123");

        for (String entityName : contractDocumentID) {
            HttpResponse response = null;
            try {
                HttpGet getRequest;
                String queryString = "/documentviewer/show/"+entityName;

                logger.debug("Query string url formed is {}", queryString);
                getRequest = new HttpGet(queryString);
                getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                getRequest.addHeader("Accept-Encoding", "gzip, deflate");

                response = super.getRequest(getRequest);
                logger.debug("Response status is {}", response.getStatusLine().toString());
                listDataJsonStr = EntityUtils.toString(response.getEntity());
                logger.debug("response json is: {}", listDataJsonStr);

                if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                        csAssert.assertTrue(true,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is able to access Contract Document"+entityName);
                }else{
                    csAssert.assertTrue(false,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is not able to access Contract Document"+entityName);
                }
            }catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
                continue;
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
                continue;
            }
            }
        csAssert.assertAll();
        }

    //Test End-User with Contract Document Access to a user via user profile page
    @Test(priority=6)
    public void testContractDocumentAccessWithReadWriteAccessForSupplierTypeUser() throws Exception {
        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(4,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        String contractIds = AccessCriteriaDbHelper.getContarctDocumentAccessId(latestUserId);

        String contractId = contractIds.replace("{","");
        String contractId1 = contractId.replace("}","");
        String[] contractDocumentID =contractId1.split(",");

        loginWithEndUser(AppUserDbHelper.getUserLoginId(latestUserId),"admin123");

        for (String entityName : contractDocumentID) {
            HttpResponse response = null;
            try {
                HttpGet getRequest;
                String queryString = "/documentviewer/show/"+entityName;

                logger.debug("Query string url formed is {}", queryString);
                getRequest = new HttpGet(queryString);
                getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                getRequest.addHeader("Accept-Encoding", "gzip, deflate");

                response = super.getRequest(getRequest);
                logger.debug("Response status is {}", response.getStatusLine().toString());
                listDataJsonStr = EntityUtils.toString(response.getEntity());
                logger.debug("response json is: {}", listDataJsonStr);

                if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                    csAssert.assertTrue(true,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is able to access Contract Document"+entityName);
                }else{
                    csAssert.assertTrue(false,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is not able to access Contract Document"+entityName);
                }
            }catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
                continue;
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
                continue;
            }
        }
        csAssert.assertAll();
    }

    //Test End-User with Contract Document Access to a user via user profile page
    @Test(priority=7)
    public void testContractDocumentAccessWithReadWriteAccessForSirionlabsTypeUser() throws Exception {
        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(1,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        String contractIds = AccessCriteriaDbHelper.getContarctDocumentAccessId(latestUserId);

        String contractId = contractIds.replace("{","");
        String contractId1 = contractId.replace("}","");
        String[] contractDocumentID =contractId1.split(",");

        loginWithEndUser(AppUserDbHelper.getUserLoginId(latestUserId),"admin123");

        for (String entityName : contractDocumentID) {
            HttpResponse response = null;
            try {
                HttpGet getRequest;
                String queryString = "/documentviewer/show/"+entityName;

                logger.debug("Query string url formed is {}", queryString);
                getRequest = new HttpGet(queryString);
                getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                getRequest.addHeader("Accept-Encoding", "gzip, deflate");

                response = super.getRequest(getRequest);
                logger.debug("Response status is {}", response.getStatusLine().toString());
                listDataJsonStr = EntityUtils.toString(response.getEntity());
                logger.debug("response json is: {}", listDataJsonStr);

                if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                    csAssert.assertTrue(true,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is able to access Contract Document"+entityName);
                }else{
                    csAssert.assertTrue(false,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is not able to access Contract Document"+entityName);
                }
            }catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
                continue;
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
                continue;
            }
        }
        csAssert.assertAll();
    }

    //Test End-User with Contract Document Access to a user via user profile page
    @Test(priority=8)
    public void testContractDocumentAccessWithReadWriteAccessForNonUserTypeUser() throws Exception {
        CustomAssert csAssert = new CustomAssert();
        int latestUserId = AppUserDbHelper.getLatestUserId(3,Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id")));

        String contractIds = AccessCriteriaDbHelper.getContarctDocumentAccessId(latestUserId);

        String contractId = contractIds.replace("{","");
        String contractId1 = contractId.replace("}","");
        String[] contractDocumentID =contractId1.split(",");

        loginWithEndUser(AppUserDbHelper.getUserLoginId(latestUserId),"admin123");

        for (String entityName : contractDocumentID) {
            HttpResponse response = null;
            try {
                HttpGet getRequest;
                String queryString = "/documentviewer/show/"+entityName;

                logger.debug("Query string url formed is {}", queryString);
                getRequest = new HttpGet(queryString);
                getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                getRequest.addHeader("Accept-Encoding", "gzip, deflate");

                response = super.getRequest(getRequest);
                logger.debug("Response status is {}", response.getStatusLine().toString());
                listDataJsonStr = EntityUtils.toString(response.getEntity());
                logger.debug("response json is: {}", listDataJsonStr);

                if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                    csAssert.assertTrue(true,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is able to access Contract Document"+entityName);
                }else{
                    csAssert.assertTrue(false,"User "+AppUserDbHelper.getUserLoginId(latestUserId) + " is not able to access Contract Document"+entityName);
                }
            }catch (SkipException e) {
                logger.error("skip exception: " + e.getMessage());
                //throw new SkipException(e.getMessage());
                continue;
            } catch (Exception e) {
                logger.error("exception error: " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
                continue;
            }
        }
        csAssert.assertAll();
    }

    @Test(priority =9)
    public void deleteAllData() throws SQLException {

        AccessCriteriaDbHelper accessCriteriaDbHelper = new AccessCriteriaDbHelper();
        accessCriteriaDbHelper.hard_delete_app_user_user_access();

        String LoginID = "User"+ DateUtils.getCurrentDateInAnyFormat("MMddyyyy");
        List<String> UserIds = AccessCriteriaDbHelper.getUserIDsFromName(LoginID);

        for(String IDs:UserIds){
                AccessCriteriaDbHelper.deleteUserDetails(IDs);
        }

        List<String> AccessCriteriaIDs = AccessCriteriaDbHelper.getAccessCriteriaIDsFromName();

        for(String IDs:AccessCriteriaIDs){
            AccessCriteriaDbHelper.deleteAccessCriteriaDetails(IDs);
        }
    }
}



