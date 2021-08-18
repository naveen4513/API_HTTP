package com.sirionlabs.test.userAccess;

import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsCreate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserCreate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import java.util.*;

public class TestUserPermissionClientAdminUSProd extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUserPermissionClientAdminUSProd.class);
    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();
    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();
    private static String loginId = "testApiAutomation"+ RandomString.getRandomAlphaNumericString(2);
    private static String uacName = "API Automation Test UAC"+RandomString.getRandomAlphaNumericString(2);

    public static Map<String, String> payloadMapUAC() {
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
        return payloadMap;
    }

    @Test
    public void testUpdateExistingURGWithAllProjectIDS(){
        CustomAssert csAssert = new CustomAssert();

        String userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
        String MasterRoleGroupConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("MasterRoleGroupConfigFileName");

        try {
            logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to update URG");
            adminHelperObj.loginWithClientAdminUser();
            Map<String, String> payloadMap = payloadMapUAC();
            Map<String,String> formData = ParseConfigFile.AllConstantPropertiesCaseSensitive(userRoleGroupFilePath, MasterRoleGroupConfigFileName, "request_payload");
            payloadMap.put("description","testurg1"+ DateUtils.getCurrentDateInDDMMYYYYHHMMSS());
            payloadMap.putAll(formData);
            String beforeUpdateHTML= String.valueOf(executor.get(MasterUserRoleGroupsCreate.getShowAPIPath()+"/5701",MasterUserRoleGroupsCreate.getHeaders()));
            int updatePageResponseCode = executor.postMultiPartFormData(MasterUserRoleGroupsCreate.postUpdateAPIPath(), MasterUserRoleGroupsCreate.getHeaders(), payloadMap).getResponse().getResponseCode();
            String afterUpdateHTML= String.valueOf(executor.get(MasterUserRoleGroupsCreate.getShowAPIPath()+"/5701",MasterUserRoleGroupsCreate.getHeaders()));
            if (updatePageResponseCode == 302) {
                if (beforeUpdateHTML.equalsIgnoreCase(afterUpdateHTML) == true) {
                    //Validate UAC Name
                    csAssert.assertTrue(false, "something went wrong" + updatePageResponseCode);
                } else {
                    csAssert.assertTrue(true, "URG got updated successfully");
                }
                csAssert.assertTrue(true,"URG updated");
            }else{
                csAssert.assertTrue(false,"Something went wrong, please contact your Administrator");
                logger.info(String.valueOf(executor.postMultiPartFormData(MasterUserRoleGroupsCreate.postUpdateAPIPath(), MasterUserRoleGroupsCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating " + TestUserPermissionClientAdminUSProd.class.getEnclosingMethod().getName() + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test
    public void testCreateURGWithAllProjectIDS(){
        CustomAssert csAssert = new CustomAssert();

        String userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
        String MasterRoleGroupConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("MasterRoleGroupConfigFileNameCreate");

        try {
            logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to update URG");
            adminHelperObj.loginWithClientAdminUser();
            Map<String, String> payloadMap = payloadMapUAC();
            Map<String,String> formData = ParseConfigFile.AllConstantPropertiesCaseSensitive(userRoleGroupFilePath, MasterRoleGroupConfigFileName, "request_payload");
            payloadMap.putAll(formData);
            payloadMap.put("name",uacName);
            payloadMap.put("description",uacName);
            int updatePageResponseCode = executor.postMultiPartFormData(MasterUserRoleGroupsCreate.getCreateAPIPath(), MasterUserRoleGroupsCreate.getHeaders(), payloadMap).getResponse().getResponseCode();
            if (updatePageResponseCode == 302) {
                csAssert.assertTrue(true,"URG Created Successfully");
            }else{
                csAssert.assertTrue(false,"Something went wrong, please contact your Administrator");
                logger.info(String.valueOf(executor.postMultiPartFormData(MasterUserRoleGroupsCreate.getCreateAPIPath(), MasterUserRoleGroupsCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating " + TestUserPermissionClientAdminUSProd.class.getEnclosingMethod().getName() + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test
    public void testUpdateEndUserWithAllProjectAccess() throws Exception {
        //TC-C44209: Update UAC of existing User.

        CustomAssert csAssert = new CustomAssert();
        String userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
        String MasterRoleGroupConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ExistingUserAllProjectAccess");
        try{
        logger.info("Updating existing User having access to all project ids");
        adminHelperObj.loginWithClientAdminUser();
        Map<String, String> payloadMap = payloadMapUAC();
        Map<String,String> formData = ParseConfigFile.AllConstantPropertiesCaseSensitive(userRoleGroupFilePath, MasterRoleGroupConfigFileName, "request_payload");
        payloadMap.putAll(formData);

        int userupdatepageresponseCode = executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseCode();

        if (userupdatepageresponseCode == 302) {
            csAssert.assertTrue(true,"User updated Successfully with all project access");
        }else{
            csAssert.assertTrue(false,"Something went wrong, please contact your Administrator");
            logger.info(String.valueOf(executor.postMultiPartFormData(UserUpdate.getApiPath(), UserUpdate.getHeaders(), payloadMap).getResponse().getResponseBody()));
        }
    } catch (Exception e) {
        csAssert.assertTrue(false, "Exception while Validating: " + TestUserPermissionClientAdminUSProd.class.getEnclosingMethod().getName() + e.getMessage());
    }
        csAssert.assertAll();
    }

    @Test
    public void testCreateEndUserWithAllProjectAccess() throws Exception {
        //TC-C44209: Update UAC of existing User.

        CustomAssert csAssert = new CustomAssert();
        String userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
        String MasterRoleGroupConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("NewUserAllProjectAccess");
        try{
            logger.info("Updating existing User having access to all project ids");
            adminHelperObj.loginWithClientAdminUser();
            Map<String, String> payloadMap = payloadMapUAC();
            Map<String,String> formData = ParseConfigFile.AllConstantPropertiesCaseSensitive(userRoleGroupFilePath, MasterRoleGroupConfigFileName, "request_payload");
            payloadMap.putAll(formData);
            payloadMap.put("firstName","API"+RandomString.getRandomAlphaNumericString(5));
            payloadMap.put("lastName","API"+RandomString.getRandomAlphaNumericString(5));
            payloadMap.put("loginId","API"+RandomString.getRandomAlphaNumericString(5));
            payloadMap.put("email","API"+RandomString.getRandomAlphaNumericString(5)+"@sirionlabs.com");
            payloadMap.put("uniqueLoginId","API"+RandomString.getRandomAlphaNumericString(5));

            int userCreateResponseCode = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse().getResponseCode();

            if (userCreateResponseCode == 302) {
                csAssert.assertTrue(true,"User Created Successfully with all project access");
            }else{
                csAssert.assertTrue(false,"Something went wrong, please contact your Administrator");
                logger.info(    String.valueOf(executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse()));
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating: " + TestUserPermissionClientAdminUSProd.class.getEnclosingMethod().getName() + e.getMessage());
        }
        csAssert.assertAll();
    }
}