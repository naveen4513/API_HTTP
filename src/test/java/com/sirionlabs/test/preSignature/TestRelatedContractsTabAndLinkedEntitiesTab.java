package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsUpdate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.api.clientSetup.provisioning.ProvisioningEdit;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestRelatedContractsTabAndLinkedEntitiesTab {

    private final static Logger logger = LoggerFactory.getLogger(TestRelatedContractsTabAndLinkedEntitiesTab.class);

    private String configFilePath = "src/test/resources/TestConfig/PreSignature/RelatedContractsTabAndLinkedEntitiesTab";
    private String configFileName = "TestRelatedContractsTabAndLinkedEntitiesTab.cfg";

    private AdminHelper adminHelperObj = new AdminHelper();
    private LinkEntity linkEntityObj = new LinkEntity();

    private int clientId;
    private int userRoleGroupId;

    @BeforeClass
    public void beforeClass() {
        clientId = adminHelperObj.getClientId();
        userRoleGroupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "urgId"));

        adminHelperObj.addPermissionForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, "711");
        adminHelperObj.addPermissionForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, "720");

        updateUserPage();
    }

    /*
    TC-C7601: Verify Link Entities permission is present on Client Setup Admin and is flowing down to client admin in correct manner.
     */
    @Test
    public void testC7601() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7601: Verify Link Entities permission is present on Client Setup Admin and is flowing down to client admin in correct manner.");

            //Validate Link Entities is Present on Client Setup Admin under Contract and Contract Draft Request.
            ClientSetupHelper setupHelperObj = new ClientSetupHelper();
            String clientName = setupHelperObj.getClientNameFromId(clientId);

            setupHelperObj.loginWithClientSetupUser();

            String provisioningEditResponse = ProvisioningEdit.getProvisioningEditResponse(clientId, clientName);
            Elements nodes = Jsoup.parse(provisioningEditResponse).getElementById("permission").child(1).child(0).children();

            boolean permissionFoundUnderContract = checkPermission("Contract", nodes, true);
            boolean permissionFoundUnderCDR = checkPermission("Contract Draft Request", nodes, true);

            if (permissionFoundUnderContract && permissionFoundUnderCDR) {
                //Validate Permissions should be present on Client Admin.
                adminHelperObj.loginWithClientAdminUser();
                String urgResponse = MasterUserRoleGroupsUpdate.getUpdateResponse(userRoleGroupId);

                nodes = Jsoup.parse(urgResponse).getElementById("permission").children();
                permissionFoundUnderContract = checkPermission("Contract", nodes, false);
                permissionFoundUnderCDR = checkPermission("Contract Draft Request", nodes, false);

                csAssert.assertTrue(permissionFoundUnderContract,
                        "Link Entities Permission not present under Contract on Client Admin even after Enabling it from Client Setup Admin.");
                csAssert.assertTrue(permissionFoundUnderCDR,
                        "Link Entities Permission not present under CDR on Client Admin even after Enabling it from Client Setup Admin.");
            } else {
                csAssert.assertEquals(permissionFoundUnderCDR, true, "Link Entities Permission not found for CDR on Client Setup Admin.");
                csAssert.assertEquals(permissionFoundUnderContract, true, "Link Entities Permission not found for Contract on Client Setup Admin.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7601. " + e.getMessage());
        } finally {
            adminHelperObj.loginWithEndUser();
        }

        csAssert.assertAll();
    }

    private boolean checkPermission(String entityName, Elements nodes, boolean clientSetupAdmin) {
        int index = clientSetupAdmin ? 3 : 5;

        for (int i = 0; i < nodes.size(); i = i + 2) {
            String entityNode = nodes.get(i).child(0).child(0).childNode(0).toString().trim().replace(":", "");

            if (entityNode.equalsIgnoreCase(entityName)) {
                Elements allPermissions = nodes.get(i + 1).child(0).child(0).child(0).child(0).children();

                for (Element permission : allPermissions) {
                    if (permission.childNode(index).toString().trim().replace("\n", "").equalsIgnoreCase("Link Entities")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /*
    TC-C7627: Verify User is able to link the entities Contract - CDR only when both have link entities permission.
     */
    @Test
    public void testC7627() {
        CustomAssert csAssert = new CustomAssert();
        Set<String> defaultPermissions = adminHelperObj.getAllPermissionsForUser(ConfigureEnvironment.getEndUserLoginId(), clientId);
        String defaultPermissionsStr = defaultPermissions.toString().replace("[", "{").replace("]", "}");

        try {
            logger.info("Starting Test TC-C7627: Verify User is able to link the entities Contract - CDR only when both have link entities permission.");

            //Disabling Link Entity Permission for CDR and Contract
            Set<String> newPermissions = new HashSet<>(defaultPermissions);
            newPermissions.remove("711");
            newPermissions.remove("720");

            String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");
            adminHelperObj.updatePermissionsForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, newPermissionsStr);

            int cdrId = ListDataHelper.getLatestRecordId("contract draft request");

            String linkEntityResponse = linkEntityObj.hitLinkEntity(160, cdrId);

            boolean contractFound = checkEntityLink(linkEntityResponse, 61);
            csAssert.assertFalse(contractFound, "Contract still coming on Manage Links under CDR Related Contracts Tab even after disabling it from Client Admin.");

            boolean cdrFound = checkEntityLink(linkEntityResponse, 160);
            csAssert.assertFalse(cdrFound, "CDR still coming on Manage Links under CDR Related Contracts Tab even after disabling it from Client Admin.");

            adminHelperObj.updatePermissionsForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, defaultPermissionsStr);

            updateUserPage();

            int contractId = ListDataHelper.getLatestRecordId("contracts");
            linkEntityResponse = linkEntityObj.hitLinkEntity(61, contractId);

            contractFound = checkEntityLink(linkEntityResponse, 61);
            cdrFound = checkEntityLink(linkEntityResponse, 160);
            csAssert.assertTrue(contractFound, "Contract not coming on Manage Links under Contract Linked Entities Tab even after enabling it from Client Admin.");
            csAssert.assertTrue(cdrFound, "CDR not coming on Manage Links under Contract Linked Entities Tab even after enabling it from Client Admin.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7627. " + e.getMessage());
        } finally {
            adminHelperObj.updatePermissionsForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, defaultPermissionsStr);
        }

        csAssert.assertAll();
    }

    private boolean checkEntityLink(String linkEntityResponse, int entityTypeId) {
        JSONObject jsonObj = new JSONObject(linkEntityResponse).getJSONObject("data");
        JSONArray jsonArr = jsonObj.getJSONArray("entityTypes");

        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getInt("id") == entityTypeId) {
                return true;
            }
        }

        return false;
    }

    /*
    TC-C7641: Verify there should not be parent child restriction while linking the contracts
    TC-C7642: Verify multiple entities can be linked from different entity type drop down
    TC-C7643: Verify the entity which got linked should also show the alternate entity in its linked entities tab with proper data
    TC-C7644: Verify that Entity of Supplier A should be linked with entity of Supplier B
     */
    @Test
    public void testC7642() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C7642: Verify multiple entities can be linked from different entity type drop down");

            if (cdrId == -1) {
                throw new Exception("Couldn't create CDR. Hence Couldn't validate further.");
            }

            //Linking Multiple Contracts (MSA, PSA, SOW, WO) with CDR
            String[] allContractShortCodeIds = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "c7642",
                    "contractShortCodeIds").trim().split(",");

            Options optionsObj = new Options();
            String linkEntitiesPayload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[";
            List<Integer> allContractIds = new ArrayList<>();

            for (String contractShortCodeId : allContractShortCodeIds) {
                //Validate able to search in Options API
                contractShortCodeId = contractShortCodeId.trim();

                Map<String, String> params = new HashMap<>();
                params.put("pageType", "8");
                params.put("entityTpeId", "160");
                params.put("query", contractShortCodeId);
                params.put("showAll", "true");

                optionsObj.hitOptions(1, params);
                String optionsResponse = optionsObj.getOptionsJsonStr();

                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                if (jsonArr.length() > 0) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        String name = jsonArr.getJSONObject(i).getString("name");

                        if (name.endsWith("(" + contractShortCodeId + ")")) {
                            linkEntitiesPayload = linkEntitiesPayload.concat("{\"entityId\":" + jsonArr.getJSONObject(i).getInt("id") + ",\"entityTypeId\":61},");
                            allContractIds.add(jsonArr.getJSONObject(i).getInt("id"));
                        }
                    }
                } else {
                    csAssert.assertFalse(true, "Unable to search Contract having ShortCodeId: " + contractShortCodeId + " at CDR Related Contracts Tab.");
                }
            }

            linkEntitiesPayload = linkEntitiesPayload.substring(0, linkEntitiesPayload.length() - 1).concat("]}");

            String linkResponse = linkEntityObj.hitLinkEntity(linkEntitiesPayload);
            if (new JSONObject(linkResponse).isNull("error")) {
                String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                        "\"orderDirection\":\"asc\",\"filterJson\":{}}}";

                //Validate all Contracts under Related Contracts Tab of CDR.
                String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 377, payload);
                JSONObject jsonObj = new JSONObject(tabListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                String contractIdColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "contract_id");

                for (int contractId : allContractIds) {
                    boolean contractFound = false;

                    for (int i = 0; i < jsonArr.length(); i++) {
                        String value = jsonArr.getJSONObject(i).getJSONObject(contractIdColumn).getString("value");

                        if (value.contains(contractId + ":;61")) {
                            contractFound = true;
                            break;
                        }
                    }

                    csAssert.assertTrue(contractFound, "Contract having Record Id " + contractId + " not found under Related Contracts Tab of CDR.");
                }

                //Validate CDR present under Linked Entities Tab of Contract.
                payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";

                for (int contractId : allContractIds) {
                    tabListResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 381, payload);

                    jsonObj = new JSONObject(tabListResponse);
                    jsonArr = jsonObj.getJSONArray("data");

                    if (jsonArr.length() > 0) {
                        String linkedEntityColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "linkedentityid");
                        String linkedValue = jsonArr.getJSONObject(0).getJSONObject(linkedEntityColumnId).getString("value");

                        if (!linkedValue.contains(cdrId + ":;160")) {
                            csAssert.assertTrue(false, "Linked Entities Tab Validation failed. Expected CDR Value: [" + cdrId +
                                    ":;160] and Actual Value: " + linkedValue);
                        }
                    } else {
                        csAssert.assertTrue(false, "No data found in Linked Entities Tab of Contract Id " + contractId);
                    }
                }
            } else {
                csAssert.assertFalse(true, "Link Entity failed.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7642. " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    private int createCDR() {
        String sectionName = "cdr creation";
        String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                sectionName, false);
        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

        if (status.equalsIgnoreCase("success")) {
            return CreateEntity.getNewEntityId(createResponse);
        } else {
            return -1;
        }
    }

    private int createContract() {
        String sectionName = "contract creation";
        String createResponse = Contract.createContract(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                sectionName, true);
        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

        if (status.equalsIgnoreCase("success")) {
            return CreateEntity.getNewEntityId(createResponse);
        } else {
            return -1;
        }
    }

    private String createContractFromCDRResponse(String newPayload) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                        "contract creation");

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        "ExtraFields.cfg");

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                    Create createObj = new Create();
                    createObj.hitCreate("contracts", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Contract Create Payload is null and hence cannot create Multi Supplier Contract.");
                }
            } else {
                logger.error("New V1 API Response is an Invalid JSON for Contracts.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }

    /*
    TC-C7648: Verify on link to the cdr with contract, the source field will show CDR name in contract entity if contract is not created from CDR.
     */
    @Test
    public void testC7648() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();
        int contractId = createContract();

        try {
            logger.info("Staring Test TC-C7648: Verify on link to the cdr with contract, the source field will show CDR name in contract entity " +
                    "if contract is not created from CDR.");

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence couldn't validate further.");
            }

            if (contractId == -1) {
                throw new Exception("Couldn't Create Contract. Hence Couldn't validate further.");
            }

            //Validate Source Name/Title is empty in contract.
            String showResponse = ShowHelper.getShowResponseVersion2(61, contractId);
            String sourceNameTitleValue = ShowHelper.getActualValue(showResponse, ShowHelper.getShowFieldHierarchy("sourcetitle", 61));

            csAssert.assertTrue(sourceNameTitleValue == null, "Source Name/Title field is not empty in Newly Created Contract.");

            //Link Contract and CDR
            String linkEntitiesPayload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\": " + contractId + ",\"entityTypeId\":61}]}";
            String linkResponse = linkEntityObj.hitLinkEntity(linkEntitiesPayload);

            if (new JSONObject(linkResponse).isNull("error")) {
                //Validate Source Name/Title in Contract is updated.
                String expectedValue = ShowHelper.getActualValue(ShowHelper.getShowResponseVersion2(160, cdrId),
                        ShowHelper.getShowFieldHierarchy("name", 160));
                String actualValue = ShowHelper.getActualValue(ShowHelper.getShowResponseVersion2(61, contractId),
                        ShowHelper.getShowFieldHierarchy("sourcetitle", 61));

                csAssert.assertEquals(expectedValue, actualValue, "Contract Source Name/Title Validation failed after Linking it with CDR.");
            } else {
                csAssert.assertFalse(true, "Link Entity failed.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7648. " + e.getMessage());
        } finally {
            if (contractId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C7649: Verify if a contract is created from CDR and it again gets linked to another CDR, source name will remain same
     */
    @Test
    public void testC7649() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId1 = createCDR();
        int cdrId2 = createCDR();
        int contractId = -1;

        String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[1024],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + cdrId1 +
                "],\"entityTypeId\":160},\"actualParentEntity\":{\"entityIds\":[1024],\"entityTypeId\":1}}";

        try {
            logger.info("Starting Test TC-C7649: Verify if a contract is created from CDR and it again gets linked to another CDR, source name will remain same");
            String createContractResponse = createContractFromCDRResponse(newPayload);
            contractId = CreateEntity.getNewEntityId(createContractResponse);

            String sourceNameTitleValue = ShowHelper.getActualValue(ShowHelper.getShowResponseVersion2(61, contractId),
                    ShowHelper.getShowFieldHierarchy("sourcetitle", 61));

            if (sourceNameTitleValue != null) {
                //Link Contract with another CDR.
                String linkEntitiesPayload = "{\"entityId\":" + cdrId2 + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\": " + contractId + ",\"entityTypeId\":61}]}";
                String linkResponse = linkEntityObj.hitLinkEntity(linkEntitiesPayload);

                if (new JSONObject(linkResponse).isNull("error")) {
                    //Validate Source Name/Title in Contract is not updated.
                    String actualValue = ShowHelper.getActualValue(ShowHelper.getShowResponseVersion2(61, contractId),
                            ShowHelper.getShowFieldHierarchy("sourcetitle", 61));

                    csAssert.assertEquals(sourceNameTitleValue, actualValue,
                            "Contract Source Name/Title Validation failed after Linking it with CDR. Value updated after Linking it with another CDR.");
                } else {
                    csAssert.assertFalse(true, "Link Entity failed.");
                }
            } else {
                csAssert.assertFalse(true, "Contract Source Name/Title value is null when created from CDR.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7649. " + e.getMessage());
        } finally {
            if (contractId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }

            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId1);
            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId2);
        }

        csAssert.assertAll();
    }

    /*
    TC-C7650: Verify user should able to remove existing links if required.
     */
    @Test
    public void testC7650() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();
        int contractId = createContract();

        try {
            logger.info("Starting Test TC-C7650: Verify user should able to remove existing links if required.");

            if (cdrId == -1) {
                throw new Exception("Couldn't create CDR. Hence couldn't validate further.");
            }

            if (contractId == -1) {
                throw new Exception("Couldn't create Contract. Hence Couldn't validate further.");
            }

            //Link Entities.
            String linkEntitiesPayload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\": " + contractId + ",\"entityTypeId\":61}]}";
            String linkResponse = linkEntityObj.hitLinkEntity(linkEntitiesPayload);

            if (new JSONObject(linkResponse).isNull("error")) {
                //Remove Link
                String payload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\":" + contractId + ",\"entityTypeId\":61}]}";
                String deleteLinkResponse = DeleteLink.getDeleteLinkResponse(payload);

                if (new JSONObject(deleteLinkResponse).isNull("error")) {
                    //Check Contract is not coming in Related Contracts Tab
                    payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                            "\"orderDirection\":\"asc\",\"filterJson\":{}}}";

                    String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 377, payload);
                    int noOfRecords = new JSONObject(tabListResponse).getJSONArray("data").length();

                    if (noOfRecords != 0) {
                        csAssert.assertFalse(true, "Contract still available under Related Contracts Tab of CDR.");
                    }
                } else {
                    csAssert.assertFalse(true, "Delete Link API failed.");
                }
            } else {
                csAssert.assertFalse(true, "Link Entity failed.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7650. " + e.getMessage());
        } finally {
            if (contractId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C7655: Verify Download button is present and working fine
     */
    @Test
    public void testC7655() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();
        int contractId = createContract();

        try {
            logger.info("Starting Test TC-C7655: Verify Download button is present and working fine");

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence couldn't validate further.");
            }

            if (contractId == -1) {
                throw new Exception("Couldn't create Contract. Hence Couldn't validate further.");
            }

            //Link Entities.
            String linkEntitiesPayload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\": " + contractId + ",\"entityTypeId\":61}]}";
            String linkResponse = linkEntityObj.hitLinkEntity(linkEntitiesPayload);

            if (new JSONObject(linkResponse).isNull("error")) {
                DownloadListWithData downloadObj = new DownloadListWithData();

                //Download CDR File
                Map<String, String> params = new HashMap<>();
                params.put("jsonData", "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                        "\"orderDirection\":\"asc\",\"filterJson\":{}}}");

                String cdrShowResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
                String contractShowResponse = ShowHelper.getShowResponseVersion2(61, contractId);

                String cdrShortCodeId = ShowHelper.getValueOfField("short code id", cdrShowResponse);
                String contractShortCodeId = ShowHelper.getValueOfField("short code id", contractShowResponse);

                HttpResponse response = downloadObj.hitDownloadTabListData(160, cdrId, 377, cdrShortCodeId, params);

                String filePath = "src/test";
                String fileName = "CDR Related Contracts.xlsx";
                boolean fileDownloaded = downloadObj.dumpDownloadListIntoFile(response, filePath, fileName);

                if (fileDownloaded) {
                    //Validate details in Excel
                    List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(filePath, fileName, "Data", 5);
                    List<String> allHeadersInLowerCase = new ArrayList<>();

                    for (String header : allHeaders) {
                        allHeadersInLowerCase.add(header.toLowerCase());
                    }

                    List<String> contractDataInExcel = XLSUtils.getExcelDataOfOneRow(filePath, fileName, "Data", 6);

                    //Validate Contract Id
                    int index = allHeadersInLowerCase.indexOf("id");
                    String actualValue = contractDataInExcel.get(index);
                    csAssert.assertEquals(actualValue, contractShortCodeId, "Contract Id Validation failed in Downloaded Excel from CDR Related Contracts Tab.");

                    //Validate Contract Title
                    index = allHeadersInLowerCase.indexOf("title");
                    actualValue = contractDataInExcel.get(index);
                    String expectedValue = ShowHelper.getActualValue(contractShowResponse, ShowHelper.getShowFieldHierarchy("title", 61));
                    csAssert.assertEquals(actualValue, expectedValue, "Contract Title validation failed in Downloaded Excel from CDR Related Contracts Tab.");

                    //Validate Type
                    index = allHeadersInLowerCase.indexOf("type");
                    actualValue = contractDataInExcel.get(index);
                    csAssert.assertTrue(actualValue.equalsIgnoreCase("Related"),
                            "Type Validation failed in Downloaded Excel from CDR Related Contracts Tab.");

                    FileUtils.deleteFile(filePath, fileName);
                } else {
                    csAssert.assertFalse(true, "Couldn't Download CDR Related Contracts Excel file.");
                }

                //Download Contract File
                params = new HashMap<>();
                params.put("jsonData", "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"asc\",\"filterJson\":{}}}");

                response = downloadObj.hitDownloadTabListData(61, contractId, 381, contractShortCodeId, params);

                fileName = "Contract Linked Entities.xlsx";
                fileDownloaded = downloadObj.dumpDownloadListIntoFile(response, filePath, fileName);

                if (fileDownloaded) {
                    //Validate details in Excel
                    List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(filePath, fileName, "Data", 5);
                    List<String> allHeadersInLowerCase = new ArrayList<>();

                    for (String header : allHeaders) {
                        allHeadersInLowerCase.add(header.toLowerCase());
                    }

                    List<String> cdrDataInExcel = XLSUtils.getExcelDataOfOneRow(filePath, fileName, "Data", 6);

                    //Validate CDR Id
                    int index = allHeadersInLowerCase.indexOf("id");
                    String actualValue = cdrDataInExcel.get(index);
                    csAssert.assertEquals(actualValue, cdrShortCodeId, "CDR Id Validation failed in Downloaded Excel from Contract Linked Entities Tab.");

                    //Validate CDR Name
                    index = allHeadersInLowerCase.indexOf("name");
                    actualValue = cdrDataInExcel.get(index);
                    String expectedValue = ShowHelper.getActualValue(cdrShowResponse, ShowHelper.getShowFieldHierarchy("name", 160));
                    csAssert.assertEquals(actualValue, expectedValue, "CDR Name validation failed in Downloaded Excel from Contract Linked Entities Tab.");

                    FileUtils.deleteFile(filePath, fileName);
                } else {
                    csAssert.assertFalse(true, "Couldn't Download Contract Linked Entities Excel file.");
                }
            } else {
                csAssert.assertFalse(true, "Link Entity failed.");
            }

        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7655. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void updateUserPage() {
        adminHelperObj.loginWithClientAdminUser();
        Map<String, String> params = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "user update payload");
        UserUpdate.hitUserUpdate(params);
        adminHelperObj.loginWithEndUser();
    }
}