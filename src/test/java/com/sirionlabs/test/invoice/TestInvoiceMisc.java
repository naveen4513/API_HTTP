package com.sirionlabs.test.invoice;

import com.sirionlabs.api.clientAdmin.masterRoleGroups.MasterRoleGroups;
import com.sirionlabs.api.clientAdmin.masterRoleGroups.MasterRoleGroupsCreate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.dbHelper.RoleGroupDbHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.swing.text.StyledEditorKit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceMisc extends TestAPIBase {

    String userrolegroups;

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceMisc.class);

    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void before(){
        String InvoiceMiscConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestInvoiceMiscPath");
        String InvoiceMiscConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestInvoiceMiscName");
        userrolegroups = ParseConfigFile.getValueFromConfigFile(InvoiceMiscConfigFilePath, InvoiceMiscConfigFileName, "userrolegroups");

    }


    @Test
    public void testC694() {
        CustomAssert csAssert = new CustomAssert();

        try {
            List<String> userData = AppUserDbHelper.getUserDataFromUserLoginId("first_name, last_name",
                    ConfigureEnvironment.getEnvironmentProperty("j_username"), 1002);

            if (userData == null || userData.isEmpty()) {
                throw new SkipException("Couldn't get User Data from DB.");
            }

            String currentUserFullName = userData.get(0) + " " + userData.get(1);

            logger.info("Hitting ListData API for Invoice.");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("invoices");

            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            JSONArray jsonArr = new JSONObject(listDataResponse).getJSONArray("data");

            boolean recordFound = false;

            for (int i = 0; i < jsonArr.length(); i++) {
                String value = jsonArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
                int recordId = ListDataHelper.getRecordIdFromValue(value);

                String createLinksResponse = CreateLinks.getCreateLinksV2Response(67, recordId);
                Map<Integer, String> createLinksMap = CreateLinks.getAllSingleCreateLinksMap(createLinksResponse);

                if (createLinksMap != null && !createLinksMap.isEmpty()) {
                    recordFound = true;

                    logger.info("Hitting Show API for Invoice Id: {}", recordId);
                    String showResponse = ShowHelper.getShowResponseVersion2(67, recordId);
                    List<String> allStakeholdersInParentInvoice = ShowHelper.getAllSelectedStakeholdersFromShowResponse(showResponse);

                    if (allStakeholdersInParentInvoice == null) {
                        throw new SkipException("Couldn't get All Selected Stakeholders in Invoice Id: " + recordId);
                    }

                    if (allStakeholdersInParentInvoice.isEmpty() || allStakeholdersInParentInvoice.contains(currentUserFullName)) {
                        continue;
                    }

                    for (Map.Entry<Integer, String> entry : createLinksMap.entrySet()) {
                        int entityTypeId = entry.getKey();

                        if (entityTypeId == 165) {
                            continue;
                        }

                        String queryString = entry.getValue();
                        String childEntityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

                        logger.info("Hitting New API for Entity {}", childEntityName);

                        New newObj = new New();
                        newObj.hitNew(childEntityName, "invoices", 67, queryString, null, null);
                        String newResponse = newObj.getNewJsonStr();

                        List<String> allSelectedStakeholdersInNewResponse = ShowHelper.getAllSelectedStakeholdersFromShowResponse(newResponse);

                        if (allSelectedStakeholdersInNewResponse == null) {
                            throw new SkipException("Couldn't Get All Selected Stakeholder Names from Child Entity New Response.");
                        }

                        allSelectedStakeholdersInNewResponse.remove(currentUserFullName);

                        for (String userInInvoice : allStakeholdersInParentInvoice) {
                            if (allSelectedStakeholdersInNewResponse.contains(userInInvoice)) {
                                csAssert.assertTrue(false, "Stakeholder " + userInInvoice + " is present in New API Response for Child Entity " +
                                        childEntityName + " of Parent Invoice Id: " + recordId);
                            }
                        }
                    }

                    break;
                }
            }

            if (!recordFound) {
                throw new SkipException("Couldn't find Invoice Record from which can create child entity.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C694: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    @Test
    public void testC677() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        logger.info("Starting Test TC-C677: Validate Creation of RG for Invoice Entity.");

        //Getting Id of Latest Role Group from DB.
        int lastRoleGroupId = RoleGroupDbHelper.getLatestRoleGroupId();

        String[] userTypeIdArr = {"1", "2", "3", "4"};

        for (String userTypeId : userTypeIdArr) {
            try {
                logger.info("Validating Creation of RG for Invoice Entity and UserTypeId: {}", userTypeId);

                adminHelperObj.loginWithClientAdminUser();

                String roleGroupName = "Test Invoice RG Create";

                Map<String, String> payloadMap = new HashMap<>();
                payloadMap.put("entityType.id", "67");
                payloadMap.put("_listReportId", "1");
                payloadMap.put("_chartIds", "1");
                payloadMap.put("validUserType", userTypeId);
                payloadMap.put("_validUserType", "1");
                payloadMap.put("name", roleGroupName);
                payloadMap.put("description", roleGroupName);
                payloadMap.put("sequenceOrder", "110099");
                payloadMap.put("_defaultLoggedInUser", "on");
                payloadMap.put("_multiValue", "on");
                payloadMap.put("_active", "on");
                payloadMap.put("_includeInVendorHierarchyGridView", "on");
                payloadMap.put("subHeader", "617");
                payloadMap.put("grantAccessOnAssign", "true");
                payloadMap.put("_grantAccessOnAssign", "on");
                payloadMap.put("userRoleGroups", userrolegroups);
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

    @Test
    public void testC955() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting TC-C955: Validate that no role group is defined for Invoice Line Item.");
            adminHelperObj.loginWithClientAdminUser();

            String createResponse = executor.get(MasterRoleGroupsCreate.getAPIPath(), MasterRoleGroupsCreate.getHeaders()).getResponse().getResponseBody();
            List<String> allEntityOptions = MasterRoleGroupsCreate.getAllEntityOptions(createResponse);

            if (allEntityOptions == null || allEntityOptions.isEmpty()) {
                throw new SkipException("Couldn't get All Entity Options from MasterRoleGroups Create API Response.");
            }

            csAssert.assertTrue(!allEntityOptions.contains("165"), "Role Group available for Invoice Line Item Entity.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C955. " + e.getMessage());
        } finally {
            new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        }
        csAssert.assertAll();
    }
}