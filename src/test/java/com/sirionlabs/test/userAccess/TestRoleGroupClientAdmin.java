package com.sirionlabs.test.userAccess;

import com.sirionlabs.api.clientAdmin.masterRoleGroups.MasterRoleGroups;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.RoleGroupDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.commonUtils.RandomString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static com.sirionlabs.utils.commonUtils.DateUtils.getCurrentDateInDDMMYYYYHHMMSS;

public class TestRoleGroupClientAdmin extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestRoleGroupClientAdmin.class);
    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();
    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();
    private static String loginId = "RoleGroup"+ getCurrentDateInDDMMYYYYHHMMSS();
    private static String uacName = "UAC"+getCurrentDateInDDMMYYYYHHMMSS();
    private static List<String> actualPermissionNamesList = new ArrayList<>();
    private static String groupName =null;
    //String userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
    //String MasterRoleGroupConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("MasterRoleGroupConfigFileName");

    private void loginWithAdminUser() {
        adminHelperObj.loginWithClientAdminUser();
    }

    public static Map<String, String> payloadMapUAC() {

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("_active", "on");
        payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
        payloadMap.put("ajax", "true");

        return payloadMap;
    }

    @Test
    public void testRoleGroup() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        logger.info("Starting Test TC-C677: Validate Creation of RG for Invoice Entity.");

        //Getting Id of Latest Role Group from DB.
        int lastRoleGroupId = RoleGroupDbHelper.getLatestRoleGroupId();

        String[] userTypeIdArr = {"1", "2", "3", "4"}; //Client(2), Sirion(1), NonUser(3), Supplier(4);

        for (String userTypeId : userTypeIdArr) {
            try {
                logger.info("Validating Creation of RG for Invoice Entity and UserTypeId: {}", userTypeId);

                loginWithAdminUser();

                String roleGroupName = "Test Invoice RG Create";

                Map<String, String> payloadMap = new HashMap<>();
                payloadMap.put("entityType.id", "67");
                payloadMap.put("_listReportId", "1");
                payloadMap.put("_chartIds", "1");
                payloadMap.put("validUserType", userTypeId);
                payloadMap.put("_validUserType", "1");
                payloadMap.put("name", roleGroupName+ RandomString.getRandomAlphaNumericString(2));
                payloadMap.put("description", roleGroupName+ RandomString.getRandomAlphaNumericString(2));
                payloadMap.put("sequenceOrder", "110099"+ RandomNumbers.getRandomNumberWithinRange(1,9));
                payloadMap.put("_defaultLoggedInUser", "on");
                payloadMap.put("_multiValue", "on");
                payloadMap.put("_active", "on");
                payloadMap.put("_includeInVendorHierarchyGridView", "on");
                payloadMap.put("subHeader", "617");
                payloadMap.put("grantAccessOnAssign", "true");
                payloadMap.put("_grantAccessOnAssign", "on");
                payloadMap.put("userRoleGroups", "1016");
                payloadMap.put("_userRoleGroups", "1");
                payloadMap.put("defaultTaskOwners", "");
                payloadMap.put("defaultTaskOwner", "");
                payloadMap.put("_userDepartments", "1");
                payloadMap.put("_groupExclusiveDepartment", "on");
                payloadMap.put("entityTypeIdsAvailingSecondaryAccess", "");
                payloadMap.put("_entityTypeIdsAvailingSecondaryAccess", "");
                payloadMap.put("ajax", "true");
                payloadMap.put("history", "{}");
                payloadMap.put("_clauseCategories","1");


                int responseCode = executor.postMultiPartFormData(MasterRoleGroups.getAPIPath(), MasterRoleGroups.getHeaders(), payloadMap).getResponse().getResponseCode();

                if (responseCode == 302) {
                    int latestRoleGroupId = RoleGroupDbHelper.getLatestRoleGroupId();

                    if (latestRoleGroupId > lastRoleGroupId) {
                        //Validate RG Name in DB.
                        List<String> rgDataInDb = RoleGroupDbHelper.getRoleGroupDataFromId("name", latestRoleGroupId);
                        csAssert.assertTrue(rgDataInDb.get(0).equalsIgnoreCase(roleGroupName), "Expected Role Group Name: " + roleGroupName +
                                " and Actual Role Group Name present in DB: " + rgDataInDb.get(0) + " for UserTypeId: " + userTypeId);

                        //Delete Role Group
                        logger.info("Deleting Role Group having Id: {}", latestRoleGroupId);
                        RoleGroupDbHelper.deleteRoleGroup(latestRoleGroupId);
                    } else {
                        csAssert.assertTrue(false, "No entry created in Role Group Table for UserTypeId: " + userTypeId);
                    }
                } else {
                    csAssert.assertTrue(false, "Couldn't create Role Group for UserTypeId: " + userTypeId);
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C677 for UserTypeId: " + userTypeId + ". " + e.getMessage());
            } finally {
                new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
            }
        }

        csAssert.assertAll();
    }
}