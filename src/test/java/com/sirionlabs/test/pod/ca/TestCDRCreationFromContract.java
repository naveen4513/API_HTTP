package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestCDRCreationFromContract extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRCreationFromContract.class);

    private Boolean permissionToCreateCDR;
    private String userLoginId;
    private Set<String> defaultPermissionIds;
    private boolean permissionChangedInDB = false;
    private int parentContractId;
    private String cdrCreationSection = "cdr creation from contract";

    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        String cdrCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRFilePath");
        String cdrCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRFileName");

        userLoginId = ConfigureEnvironment.getEnvironmentProperty("j_username");
        defaultPermissionIds = adminHelperObj.getAllPermissionsForUser(userLoginId, 1002);

        permissionToCreateCDR = (defaultPermissionIds != null && defaultPermissionIds.contains("504"));

        parentContractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(cdrCreationConfigFilePath, cdrCreationConfigFileName,
                cdrCreationSection, "sourceid"));
    }

    @AfterClass
    public void afterClass() {
        //Restore Permissions to default.
        if (permissionChangedInDB) {
            String allPermissionsStr = defaultPermissionIds.toString().replace("[", "{").replace("]", "}");
            adminHelperObj.updatePermissionsForUser(userLoginId, 1002, allPermissionsStr);
        }

        updateUserPage();
    }

    private void enablePermissionToCreateCDR() {
        Set<String> allPermissionIds = new HashSet<>(defaultPermissionIds);
        allPermissionIds.add("504");

        String allPermissionsStr = allPermissionIds.toString().replace("[", "{").replace("]", "}");
        boolean permissionEnabled = adminHelperObj.updatePermissionsForUser(userLoginId, 1002, allPermissionsStr);

        updateUserPage();

        if (permissionEnabled) {
            permissionToCreateCDR = true;
            permissionChangedInDB = true;
        }
    }

    private void disablePermissionToCreateCDR() {
        Set<String> allPermissionIds = new HashSet<>(defaultPermissionIds);

        allPermissionIds.remove("504");

        String allPermissionsStr = allPermissionIds.toString().replace("[", "{").replace("]", "}");
        boolean permissionDisabled = adminHelperObj.updatePermissionsForUser(userLoginId, 1002, allPermissionsStr);

        if (permissionDisabled) {
            permissionToCreateCDR = false;
            permissionChangedInDB = true;
        }
    }


    /*
    TC-C63077: Verify that Option to Create CDR from Contract is shown if user has permission enabled from URG.
     */
    @Test(priority = 1)
    public void testC63077() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C63077: Verify that Option to Create CDR from Contract is shown if user has permission enabled from URG.");

            if (!permissionToCreateCDR) {
                enablePermissionToCreateCDR();

                if (!permissionToCreateCDR) {
                    throw new SkipException("Couldn't Enable Permission to Create CDR in DB.");
                }
            }

            String showResponse = ShowHelper.getShowResponse(61, parentContractId);
            Boolean hasCreateOption = ShowHelper.hasCreateOptionForEntity(showResponse, "contract draft request");

            if (hasCreateOption == null) {
                throw new SkipException("Couldn't check whether the Contract Id " + parentContractId + " has Option to Create CDR or not.");
            }

            csAssert.assertTrue(hasCreateOption, "Contract Id " + parentContractId + " doesn't have Option to Create CDR on Show Page.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C63077. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C63080: Verify that Option to Create CDR from Contract is not shown if user doesn't have permission enabled from URG.
     */
    @Test
    public void testC63080() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C36080: Verify that Option to Create CDR from Contract is not shown if user doesn't have permission enabled from URG.");

            if (permissionToCreateCDR) {
                disablePermissionToCreateCDR();

                if (permissionToCreateCDR) {
                    throw new SkipException("Couldn't Disable Permission to Create CDR in DB.");
                }
            }

            String showResponse = ShowHelper.getShowResponse(61, parentContractId);
            Boolean hasCreateOption = ShowHelper.hasCreateOptionForEntity(showResponse, "contract draft request");

            if (hasCreateOption == null) {
                throw new SkipException("Couldn't check whether the Contract Id " + parentContractId + " has Option to Create CDR or not.");
            }

            csAssert.assertTrue(!hasCreateOption, "Contract Id " + parentContractId + " has Option to Create CDR on Show Page.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C63080. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    @Test(dependsOnMethods = "testC63077")
    public void testCreateCDRFromContract() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating CDR Creation from Contract.");
            String showResponse = ShowHelper.getShowResponse(61, parentContractId);
            String apiPath = ShowHelper.getCreateLinkForEntity(showResponse, 160, null);

            String newResponse = executor.get(apiPath, ApiHeaders.getDefaultLegacyHeaders()).getResponse().getResponseBody();

            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                validatePreSelectedFieldsOnCreatePage(newResponse, csAssert);

                //Create CDR after changing Default Supplier.
                String createResponse = ContractDraftRequest.createCDR(cdrCreationSection, true);

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    String status = ParseJsonResponse.getStatusFromResponse(createResponse);

                    if (status.equalsIgnoreCase("success")) {
                        int cdrId = CreateEntity.getNewEntityId(createResponse);

                        validateLinkedEntitiesTab(cdrId, csAssert);

                        validateRelatedContractsTab(cdrId, csAssert);

                        EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
                    } else {
                        csAssert.assertTrue(false, "CDR Creation failed due to " + status);
                    }
                } else {
                    csAssert.assertTrue(false, "Create Response is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "New API Response for CDR from Contract Id " + parentContractId + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating CDR Creation from Contract. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    /*
    TC-C63081: Verify that CDR create page opens.
    TC-C63082: Verify that supplier should come pre-selected on CDR create page.
    TC-C63107: Verify Source Name/Title field is populated as Contract on CDR create page.
     */
    private void validatePreSelectedFieldsOnCreatePage(String newResponse, CustomAssert csAssert) {
        try {
            //Validate Supplier Field.
            logger.info("Validating Supplier Field on Create Page.");
            JSONObject jsonObj = new JSONObject(newResponse).getJSONObject("body").getJSONObject("data");
            JSONArray jsonArr = jsonObj.getJSONObject("suppliers").getJSONArray("values");

            String actualSupplierName = jsonArr.getJSONObject(0).getString("name");

            String showResponse = ShowHelper.getShowResponse(61, parentContractId);
            JSONObject jsonObject = new JSONObject(showResponse);
            String expectedSupplierName = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("relations").getJSONArray("values").getJSONObject(0).get("name").toString();

            if (expectedSupplierName == null || !expectedSupplierName.equalsIgnoreCase(actualSupplierName)) {
                csAssert.assertTrue(false, "Supplier Validation failed on Create Page of CDR. Expected Supplier Name: [" + expectedSupplierName +
                        "] and Actual Supplier Name: [" + actualSupplierName + "]");
            }

            //Validate Source Field.
            String actualSourceName = jsonObj.getJSONObject("sourceTitle").getJSONObject("values").getString("name");
            String expectedSourceName = ShowHelper.getValueOfField("name", showResponse);

            if (expectedSourceName == null || !expectedSourceName.equalsIgnoreCase(actualSourceName)) {
                csAssert.assertTrue(false, "Source Validation failed on Create Page of CDR. Expected Source Name: [" + expectedSourceName +
                        "] and Actual Source Name: [" + actualSourceName + "]");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Pre-Selected Fields on Create Page of CDR. " + e.getMessage());
        }
    }

    /*
    TC-C63084: Verify that Newly Created CDR is coming in Linked Entities Tab of Contract.
     */
    private void validateLinkedEntitiesTab(int cdrId, CustomAssert csAssert) {
        try {
            logger.info("Validating Linked Entities Tab of Contract.");
            String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(61, parentContractId, 381, payload);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject jsonObj = new JSONObject(tabListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                if (jsonArr.length() > 0) {
                    String linkedEntityColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "linkedentityid");
                    String linkedValue = jsonArr.getJSONObject(0).getJSONObject(linkedEntityColumnId).getString("value");

                    if (!linkedValue.contains(cdrId + ":;160")) {
                        csAssert.assertTrue(false, "Linked Entities Tab Validation failed. Expected CDR Value: [" + cdrId +
                                ":;160] and Actual Value: " + linkedValue);
                    }
                } else {
                    csAssert.assertTrue(false, "No data found in Linked Entities Tab of Contract Id " + parentContractId);
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Linked Entities Tab of Contract Id " + parentContractId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Linked Entities Tab of Contract. " + e.getMessage());
        }
    }

    /*
    TC-C63085: Verify Parent Contract is coming in Related Contracts Tab of Newly Created CDR.
    TC-C63106: Verify that the Type in Related Contracts Tab of CDR is 'Related'
     */
    private void validateRelatedContractsTab(int cdrId, CustomAssert csAssert) {
        try {
            logger.info("Validating Related Contracts Tab of CDR.");
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 377, payload);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject jsonObj = new JSONObject(tabListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                if (jsonArr.length() > 0) {
                    String contractColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "contract_id");
                    String contractValue = jsonArr.getJSONObject(0).getJSONObject(contractColumnId).getString("value");

                    if (!contractValue.contains(parentContractId + ":;61")) {
                        csAssert.assertTrue(false, "Related Contracts Tab Validation failed. Expected Contract Value: [" + parentContractId +
                                ":;61] and Actual Value: " + contractValue);
                    }

                    String contractTypeColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "contract_type");
                    String contractTypeValue = jsonArr.getJSONObject(0).getJSONObject(contractTypeColumnId).getString("value");

                    if (!contractTypeValue.equalsIgnoreCase("Related")) {
                        csAssert.assertTrue(false, "Type field validation failed in Related Contracts Tab of CDR Id " + cdrId +
                                ". Expected Value: Related and Actual Value: " + contractTypeValue);
                    }
                } else {
                    csAssert.assertTrue(false, "No data found in Related Contracts Tab of CDR Id " + cdrId);
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Related Contracts Tab of CDR Id " + cdrId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Related Contracts Tab of CDR. " + e.getMessage());
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