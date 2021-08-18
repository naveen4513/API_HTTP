package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Issue;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

public class TestSIR149687ActionAndIssueCreationFromCDR extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestSIR149687ActionAndIssueCreationFromCDR.class);

    private String userLoginId;
    private Set<String> defaultPermissionIds;
    private boolean permissionChangedInDB = false;
    private int parentCDRId;
    private String actionCreationSection = "action creation from cdr";
    private String issueCreationSection = "issue creation from cdr";

    private List<String> entities = new ArrayList<>(Arrays.asList("Actions", "Issues"));

    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        String actionCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ActionFilePath");
        String actionCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionFileName");

        userLoginId = ConfigureEnvironment.getEnvironmentProperty("j_username");
        defaultPermissionIds = adminHelperObj.getAllPermissionsForUser(userLoginId, 1002);

        parentCDRId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(actionCreationConfigFilePath, actionCreationConfigFileName,
                actionCreationSection, "sourceid"));
    }

    @AfterClass
    public void afterClass() {
        //Restore Permissions to default.
        if (permissionChangedInDB) {
            String allPermissionsStr = defaultPermissionIds.toString().replace("[", "{").replace("]", "}");
            adminHelperObj.updatePermissionsForUser(userLoginId, 1002, allPermissionsStr);
        }
    }

    private void disablePermissionsToCreateEntity() {
        Set<String> allPermissionIds = new HashSet<>(defaultPermissionIds);

        allPermissionIds.remove("95");
        allPermissionIds.remove("55");

        String allPermissionsStr = allPermissionIds.toString().replace("[", "{").replace("]", "}");
        boolean permissionDisabled = adminHelperObj.updatePermissionsForUser(userLoginId, 1002, allPermissionsStr);

        if (permissionDisabled) {
            permissionChangedInDB = true;
        }
    }

    private void enablePermissionsToCreateEntity() {
        Set<String> allPermissionIds = new HashSet<>(defaultPermissionIds);
        allPermissionIds.add("95");
        allPermissionIds.add("55");

        String allPermissionsStr = allPermissionIds.toString().replace("[", "{").replace("]", "}");
        boolean permissionEnabled = adminHelperObj.updatePermissionsForUser(userLoginId, 1002, allPermissionsStr);

        updateUserPage();

        if (permissionEnabled) {
            permissionChangedInDB = true;
        }
    }


    /*
    TC-C76541: Verify that Option to Create Action from CDR is not shown if user doesn't have permission enabled from URG.
    TC-C76578: Verify that Option to Create Issue from CDR is not shown if user doesn't have permission enabled from URG.
     */
    @Test
    public void testC76541() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: Verify that Option to Create Entities Action, Issue from CDR is not shown if user doesn't have permission enabled from URG.");
            disablePermissionsToCreateEntity();

            String createLinksResponse = CreateLinks.getCreateLinksV2Response(160, parentCDRId);
            Boolean hasCreateOption = CreateLinks.hasCreateOptionForEntity(createLinksResponse, "actions");

            if (hasCreateOption == null) {
                throw new Exception("Couldn't check whether the CDR Id " + parentCDRId + " has Option to Create Entity Actions or not.");
            }

            csAssert.assertTrue(!hasCreateOption, "CDR Id " + parentCDRId + " has Option to Create Entity Actions on Show Page.");

            hasCreateOption = CreateLinks.hasCreateOptionForEntity(createLinksResponse, "issues");

            if (hasCreateOption == null) {
                throw new Exception("Couldn't check whether the CDR Id " + parentCDRId + " has Option to Create Entity Issues or not.");
            }

            csAssert.assertTrue(!hasCreateOption, "CDR Id " + parentCDRId + " has Option to Create Entity Issues on Show Page.");
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C76541. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C76540: Verify that Option to Create Action from CDR is shown if user has permission enabled from URG.
    TC-C76577: Verify that Option to Create Issue from CDR is shown if user has permission enabled from URG.
     */
    @Test(priority = 1)
    public void testC76540() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: Verify that Option to Create Action, Issue from CDR is shown if user has permission enabled from URG.");
            enablePermissionsToCreateEntity();

            String showResponse = ShowHelper.getShowResponse(160, parentCDRId);
            Boolean hasCreateOption = ShowHelper.hasCreateOptionForEntity(showResponse, "actions");

            if (hasCreateOption == null) {
                throw new Exception("Couldn't check whether the CDR Id " + parentCDRId + " has Option to Create Entity Actions or not.");
            }

            csAssert.assertTrue(hasCreateOption, "CDR Id " + parentCDRId + " doesn't have Option to Create Entity Actions on Show Page.");

            hasCreateOption = ShowHelper.hasCreateOptionForEntity(showResponse, "issues");

            if (hasCreateOption == null) {
                throw new Exception("Couldn't check whether the CDR Id " + parentCDRId + " has Option to Create Entity Issues or not.");
            }

            csAssert.assertTrue(hasCreateOption, "CDR Id " + parentCDRId + " doesn't have Option to Create Entity Issues on Show Page.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C76540. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    @Test(dependsOnMethods = "testC76540")
    public void testCreateEntityFromCDR() {
        CustomAssert csAssert = new CustomAssert();

        for (String entityName : entities) {
            try {
                logger.info("Validating Entity {} Creation from CDR.", entityName);
                String showResponse = ShowHelper.getShowResponse(160, parentCDRId);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                String apiPath = ShowHelper.getCreateLinkForEntity(showResponse, entityTypeId, null);

                String newResponse = executor.get(apiPath, ApiHeaders.getDefaultLegacyHeaders()).getResponse().getResponseBody();

                if (ParseJsonResponse.validJsonResponse(newResponse)) {
                    validatePreSelectedFieldsOnCreatePage(entityName, newResponse, csAssert);

                    //Create Entity after changing Default Supplier.
                    String sectionName = entityName.equalsIgnoreCase("actions") ? actionCreationSection : issueCreationSection;
                    String createResponse;

                    if (entityName.equalsIgnoreCase("actions")) {
                        createResponse = Action.createAction(sectionName, false);
                    } else {
                        createResponse = Issue.createIssue(sectionName, false);
                    }

                    if (ParseJsonResponse.validJsonResponse(createResponse)) {
                        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

                        if (status.equalsIgnoreCase("success")) {
                            int newlyCreatedRecordId = CreateEntity.getNewEntityId(createResponse);

                            //Validate CDR Forward References Tab
                            validateForwardReferencesTabInCDR(entityName, entityTypeId, newlyCreatedRecordId, csAssert);

                            //Validate Source Reference Tab
                            validateSourceReferenceTab(entityName, entityTypeId, newlyCreatedRecordId, csAssert);

                            EntityOperationsHelper.deleteEntityRecord(entityName, newlyCreatedRecordId);
                        } else {
                            csAssert.assertTrue(false, "Entity " + entityName + " Creation failed due to " + status);
                        }
                    } else {
                        csAssert.assertTrue(false, "Create Response for Entity " + entityName + " is an Invalid JSON.");
                    }
                } else {
                    csAssert.assertTrue(false, "New API Response for Entity " + entityName + " from CDR Id " + parentCDRId + " is an Invalid JSON.");
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Entity " + entityName + " Creation from CDR. " + e.getMessage());
            }
        }
        csAssert.assertAll();
    }

    /*
    TC-C76543: Verify that Source & Title fields should get pre-selected on Action Create Page.
    TC-C76580: Verify that Source & Title fields should get pre-selected on Issue Create Page.
     */
    private void validatePreSelectedFieldsOnCreatePage(String entityName, String newResponse, CustomAssert csAssert) {
        try {
            //Validate Supplier Field.
            logger.info("Validating Supplier Field on Create Page for Entity {}", entityName);
            JSONObject jsonObj = new JSONObject(newResponse).getJSONObject("body").getJSONObject("data");
            JSONArray jsonArr = jsonObj.getJSONObject("supplier").getJSONObject("options").getJSONArray("data");

            String showResponse = ShowHelper.getShowResponse(160, parentCDRId);

            List<String> actualSupplierNamesList = new ArrayList<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                String actualSupplierName = jsonArr.getJSONObject(i).getString("name");
                actualSupplierNamesList.add(actualSupplierName.trim().toLowerCase());
            }

            String fieldHierarchy = ShowHelper.getShowFieldHierarchy("suppliers", 160);
            List<String> expectedSupplierNamesList = ShowHelper.getAllSelectValuesOfField(showResponse, "suppliers", fieldHierarchy,
                    parentCDRId, 160);

            if (expectedSupplierNamesList == null || expectedSupplierNamesList.isEmpty()) {
                throw new SkipException("Couldn't Get Expected Supplier Names List from Show Page of CDR Id " + parentCDRId);
            }

            for (String expectedSupplierName : expectedSupplierNamesList) {
                if (!actualSupplierNamesList.contains(expectedSupplierName)) {
                    csAssert.assertTrue(false, "Supplier Validation failed on Create Page of Entity " + entityName + ". Expected Supplier Name: [" +
                            expectedSupplierName + "] not found in Supplier Options.");
                }
            }

            //Validate Source Name/Title Field.
            String actualSourceName = jsonObj.getJSONObject("sourceTitle").getJSONObject("values").getString("name");
            String expectedSourceName = ShowHelper.getValueOfField("title", showResponse);

            if (expectedSourceName == null || !expectedSourceName.equalsIgnoreCase(actualSourceName)) {
                csAssert.assertTrue(false, "Source Name/Title Validation failed on Create Page of Entity " + entityName + ". Expected Source Name: [" +
                        expectedSourceName + "] and Actual Source Name: [" + actualSourceName + "]");
            }

            //Validate Source Field.
            int actualParentEntityTypeId = jsonObj.getJSONObject("parentEntityType").getJSONObject("values").getInt("id");

            if (actualParentEntityTypeId != 160) {
                csAssert.assertTrue(false, "Source Field Validation failed on Create Page of Entity " + entityName +
                        ". Expected Source EntityTypeId: 160 and Actual Source EntityTypeId: " + actualParentEntityTypeId);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Pre-Selected Fields on Create Page of Entity " + entityName + ". " + e.getMessage());
        }
    }

    /*
    Below method covers following cases:
    TC-C76545: Verify that Newly Created Action is present in Forward References Tab of CDR.
    TC-C76581: Verify that Newly Created Issue is present in Forward References Tab of CDR.
     */
    private void validateForwardReferencesTabInCDR(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Forward References Tab of CDR for Entity {}", entityName);
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(160, parentCDRId, 66, payload);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject jsonObj = new JSONObject(tabListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                List<String> values = new LinkedList<>();
                if (jsonArr.length() > 0) {
                    String entityColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "id");
                    int entitiesLength = jsonArr.length();
                    for (int i = 0; i < entitiesLength; i++) {
                        values.add(jsonArr.getJSONObject(i).getJSONObject(entityColumnId).getString("value"));
                    }

                    values = values.stream().map(m -> m.trim()).filter(m -> m.contains(recordId + ":;" + entityTypeId)).collect(Collectors.toList());
                    if (!values.get(0).contains(recordId + ":;" + entityTypeId)) {
                        csAssert.assertTrue(false, "Forward References Tab Validation failed for Entity " + entityName +
                                ". Expected Record Value: [" + recordId + ":;" + entityTypeId + "] and Actual Value: " + values.get(0));
                    }
                } else {
                    csAssert.assertTrue(false, "No data found in Forward References Tab of CDR Id " + parentCDRId + " for Entity " + entityName);
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Forward References Tab of CDR Id " + parentCDRId +
                        " is an Invalid JSON for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Forward References Tab of CDR for Entity " + entityName + ". " + e.getMessage());
        }
    }

    /*
    TC-C76547: Verify that Parent CDR is coming in Source Reference Tab of Action
    TC-C76548: Verify that correct CDR information is coming in Source Reference Tab of Action
    TC-C76583: Verify that Parent CDR is coming in Source Reference Tab of Issue
    TC-C76584: Verify that correct CDR information is coming in Source Reference Tab of Issue
     */
    private void validateSourceReferenceTab(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Source Reference Tab of Entity {}", entityName);
            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
                    "\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(entityTypeId, recordId, 67, payload);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject jsonObj = new JSONObject(tabListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                if (jsonArr.length() > 0) {
                    String entityColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "id");
                    String value = jsonArr.getJSONObject(0).getJSONObject(entityColumnId).getString("value");

                    if (!value.contains(parentCDRId + ":;160")) {
                        csAssert.assertTrue(false, "Source Reference Tab Validation failed for Entity " + entityName +
                                ". Expected Record Value: [" + parentCDRId + ":;160] and Actual Value: " + value);
                    }
                } else {
                    csAssert.assertTrue(false, "No data found in Source Reference Tab of Entity " + entityName);
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Source Reference Tab of Entity " + entityName + " is an Invalid JSON");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Source References Tab of Entity " + entityName + ". " + e.getMessage());
        }
    }

    private void updateUserPage() {
        String configFilePath = "src/test/resources/TestConfig/PreSignature/RelatedContractsTabAndLinkedEntitiesTab";
        String configFileName = "TestRelatedContractsTabAndLinkedEntitiesTab.cfg";

        adminHelperObj.loginWithClientAdminUser();
        Map<String, String> params = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "user update payload");
        UserUpdate.hitUserUpdate(params);
        adminHelperObj.loginWithEndUser();
    }
}