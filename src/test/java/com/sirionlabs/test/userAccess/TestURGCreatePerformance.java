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

import java.util.HashMap;
import java.util.Map;

public class TestURGCreatePerformance extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestURGCreatePerformance.class);
    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();
    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();

    public static Map<String, String> payloadMapUAC() {
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
        return payloadMap;
    }

    @Test
    public void testCreateURGWithAllProjectIDS() {
        CustomAssert csAssert = new CustomAssert();

        String userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
        String MasterRoleGroupConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("MasterUserRoleGroupCreatePerformance");

        for (int i = 1800; i <= 15000; i++) {
            try {
                logger.info("Creating URG Number " + i);
                logger.info("Starting Test TC-C42208: Validating that Client Admin should be able to update URG");
                adminHelperObj.loginWithClientAdminUser();
                Map<String, String> payloadMap = payloadMapUAC();
                Map<String, String> formData = ParseConfigFile.AllConstantPropertiesCaseSensitive(userRoleGroupFilePath, MasterRoleGroupConfigFileName, "request_payload");
                payloadMap.putAll(formData);
                String uacName = "API Automation"+RandomString.getRandomAlphaNumericString(8)+DateUtils.getCurrentDateInDDMMYYYYHHMMSS();
                payloadMap.put("name", uacName);
                payloadMap.put("description", uacName);
                int updatePageResponseCode = executor.postMultiPartFormData(MasterUserRoleGroupsCreate.getCreateAPIPath(), MasterUserRoleGroupsCreate.getHeaders(), payloadMap).getResponse().getResponseCode();
                if (updatePageResponseCode == 302) {
                    csAssert.assertTrue(true, "URG Created Successfully");
                } else {
                    csAssert.assertTrue(false, "Something went wrong, please contact your Administrator");
                    logger.info(String.valueOf(executor.postMultiPartFormData(MasterUserRoleGroupsCreate.getCreateAPIPath(), MasterUserRoleGroupsCreate.getHeaders(), payloadMap).getResponse().getResponseBody()));
                }
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating " + TestURGCreatePerformance.class.getEnclosingMethod().getName() + e.getMessage());
            }
            csAssert.assertAll();
        }
    }

}