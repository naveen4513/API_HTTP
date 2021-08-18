package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.clientAdmin.templateType.CreateClientTemplateType;
import com.sirionlabs.api.clientAdmin.templateType.TemplateTypeList;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.documentFlow.MoveToTreeSave;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestContractDocumentTab {

    private final static Logger logger = LoggerFactory.getLogger(TestContractDocumentTab.class);

    private String configFilePath = "src/test/resources/TestConfig/PreSignature/ContractDocumentTab";
    private String configFileName = "TestCDRContractDocumentTab.cfg";

    private int clientId;

    private SubmitDraft submitDraftObj = new SubmitDraft();
    private AdminHelper adminHelperObj = new AdminHelper();
    private Edit editObj = new Edit();

    @BeforeClass
    public void beforeClass() {
        clientId = adminHelperObj.getClientId();
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

    /*
    TC-C40850: Verify Template Type Options
     */
    @Test
    public void testC40850() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C40850: Verify Template Type Options");
            adminHelperObj.loginWithClientAdminUser();

            String templateTypeListResponse = TemplateTypeList.getTemplateTypeListResponse();
            List<String> allExpectedTemplateTypes = TemplateTypeList.getAllActiveTemplateTypes(templateTypeListResponse);

            if (allExpectedTemplateTypes == null) {
                csAssert.assertFalse(true, "Couldn't get All Template Types from Client Admin.");
            }

            adminHelperObj.loginWithEndUser();

            DefaultUserListMetadataHelper metadataHelperObj = new DefaultUserListMetadataHelper();
            Map<String, String> params = new HashMap<>();
            params.put("entityTypeId", "160");
            String defaultUserListResponse = metadataHelperObj.getDefaultUserListMetadataResponse(367, params);
            List<String> allActualTemplateTypes = metadataHelperObj.getAllTemplateTypeList(defaultUserListResponse);

            if (allExpectedTemplateTypes.size() == allActualTemplateTypes.size()) {
                for (String expectedTemplateType : allExpectedTemplateTypes) {
                    if (!allActualTemplateTypes.contains(expectedTemplateType)) {
                        csAssert.assertFalse(true, "Expected Template Type: " + expectedTemplateType +
                                " not found in DefaultUserListMetadata API Response.");
                    }
                }
            } else {
                csAssert.assertFalse(true, "Template Types Options Validation failed. Total No of Active Template Types on Client Admin: " +
                        allExpectedTemplateTypes.size() + " and No of Template Types coming in DefaultUserListMetadata API Response: " + allActualTemplateTypes.size());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C40850. " + e.getMessage());
        } finally {
            adminHelperObj.loginWithEndUser();
        }

        csAssert.assertAll();
    }

    /*
    TC-C42075: Verify that Newly Created and Active Template Type is available to End User.
     */
    @Test
    public void testC42075() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C42075: Verify Newly Created and Active Template Type is available to End User.");
            adminHelperObj.loginWithClientAdminUser();

            String newTemplateName = "Automation Template Type";
            Map<String, String> params = CreateClientTemplateType.getParamsMap("1011", newTemplateName, true);
            int createResponse = CreateClientTemplateType.hitCreateClientTemplateType(params);

            if (createResponse == 302) {
                adminHelperObj.loginWithEndUser();

                DefaultUserListMetadataHelper metadataHelperObj = new DefaultUserListMetadataHelper();
                params = new HashMap<>();
                params.put("entityTypeId", "160");
                String defaultUserListResponse = metadataHelperObj.getDefaultUserListMetadataResponse(367, params);
                List<String> allActualTemplateTypes = metadataHelperObj.getAllTemplateTypeList(defaultUserListResponse);

                if (!allActualTemplateTypes.contains(newTemplateName)) {
                    csAssert.assertFalse(true, "Newly Created Template Type [" + newTemplateName + "] not found in DefaultUserListMetadata API Response.");
                }

                PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
                String query = "delete from template_type where client_id = " + clientId + " and name = '" + newTemplateName + "'";
                sqlObj.deleteDBEntry(query);
            } else {
                csAssert.assertFalse(true, "Template Type Creation at Client Admin failed. Hence couldn't validate further.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C42075. " + e.getMessage());
        } finally {
            adminHelperObj.loginWithEndUser();
        }

        csAssert.assertAll();
    }

    /*
    TC-C42175: Verify that Change of Template Type at latest version updates all corresponding versions.
     */
    @Test
    public void testC42175() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C42175: Verify that Change of Template Type at latest version updates all corresponding versions.");

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence couldn't validate further.");
            }

            //Upload 1st Version with Type as Main Template
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(160, cdrId, "Sample.txt", randomKeyForFileUpload);

            JSONObject jsonObj = new JSONObject(draftResponse);

            String payload = getPayloadForSubmitDraft(160, cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                    1001, 1);
            submitDraftObj.hitSubmitDraft(payload);

            //Upload 2nd Version with Type as Main Template
            randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            draftResponse = uploadDocument(160, cdrId, "Sample.txt", randomKeyForFileUpload);

            jsonObj = new JSONObject(draftResponse);

            payload = getPayloadForSubmitDraft(160, cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                    1001, 2);
            submitDraftObj.hitSubmitDraft(payload);

            //Validate Audit log
            validateAuditLog(160, cdrId, "TC-C42175", csAssert);

            //Update Type of 2nd Version as Attachment
            if (updateRecord("contract draft request", cdrId, jsonObj.get("documentFileId").toString())) {
                payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                String tabListDataResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, payload);

                JSONArray jsonArr = new JSONObject(tabListDataResponse).getJSONArray("data");
                String typeColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "type");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String value = jsonArr.getJSONObject(i).getJSONObject(typeColumn).getString("value");

                    if (!value.equalsIgnoreCase("Attachment")) {
                        String versionColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "version");

                        csAssert.assertFalse(true, "Template Type Validation failed for Document having Version " +
                                jsonArr.getJSONObject(i).getJSONObject(versionColumn).getString("value") + ". Expected Value: Attachment and Actual Value: " + value);
                    }
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Update Template Type in CDR. Hence Couldn't validate further.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C42175. " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C42028: Verify that via Upload - Change in Template Type Updates all corresponding versions in cluster in CDR.
    TC-C42017: Verify that Updated Template type is reflecting in downloaded excel.
     */
    @Test
    public void testC42028() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C42028: Verify that via Upload - Change in Template Type Updates all corresponding versions in cluster");

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence couldn't validate further.");
            }

            //Upload 1st Version with Type as Main Template
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(160, cdrId, "Sample.txt", randomKeyForFileUpload);

            JSONObject jsonObj = new JSONObject(draftResponse);

            String payload = getPayloadForSubmitDraft(160, cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                    1001, 1);
            submitDraftObj.hitSubmitDraft(payload);

            //Upload 2nd Version with Type as Attachment
            randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            draftResponse = uploadDocument(160, cdrId, "Sample.txt", randomKeyForFileUpload);

            jsonObj = new JSONObject(draftResponse);

            payload = getPayloadForSubmitDraft(160, cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                    1002, 2);
            submitDraftObj.hitSubmitDraft(payload);

            payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListDataResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, payload);

            JSONArray jsonArr = new JSONObject(tabListDataResponse).getJSONArray("data");
            String typeColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "type");

            for (int i = 0; i < jsonArr.length(); i++) {
                String value = jsonArr.getJSONObject(i).getJSONObject(typeColumn).getString("value");

                if (!value.equalsIgnoreCase("Attachment")) {
                    String versionColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "version");

                    csAssert.assertFalse(true, "Template Type Validation failed for Document having Version " +
                            jsonArr.getJSONObject(i).getJSONObject(versionColumn).getString("value") + ". Expected Value: Attachment and Actual Value: " + value);
                }
            }

            //Validate Audit log
            validateAuditLog(160, cdrId, "TC-C42028", csAssert);

            //Validate TC-C42017
            validateC42017(cdrId, csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C42028. " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    private void validateC42017(int cdrId, CustomAssert csAssert) {
        try {
            logger.info("Validating TC-C42017: Verify that Updated Template type is reflecting in downloaded excel.");
            DownloadListWithData downloadObj = new DownloadListWithData();
            Map<String, String> params = new HashMap<>();
            params.put("jsonData", "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}");

            String showResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
            String shortCodeId = ShowHelper.getValueOfField(160, "short code id", showResponse);
            HttpResponse response = downloadObj.hitDownloadTabListData(160, cdrId, 367, shortCodeId, params);

            String filePath = "src/test";
            String fileName = "C42017.xlsx";
            boolean fileDownloaded = downloadObj.dumpDownloadListIntoFile(response, filePath, fileName);

            if (fileDownloaded) {
                List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(filePath, fileName, "Data", 5);
                List<String> allHeadersInLowerCase = new ArrayList<>();

                for (String header : allHeaders) {
                    allHeadersInLowerCase.add(header.toLowerCase());
                }

                if (allHeadersInLowerCase.contains("type")) {
                    int index = allHeadersInLowerCase.indexOf("type");
                    Long noOfRows = XLSUtils.getNoOfRows(filePath, fileName, "Data");
                    Set<String> allTypeValues = new HashSet<>(XLSUtils.getOneColumnDataFromMultipleRows(filePath, fileName, "Data", index, 5,
                            noOfRows.intValue() - 7));

                    if (allTypeValues.size() != 1) {
                        if (allTypeValues.size() > 1) {
                            csAssert.assertFalse(true, "Template Type for all the documents are not same in Downloaded Excel.");
                        } else {
                            csAssert.assertFalse(true, "Couldn't locate Template Type values in Downloaded Excel.");
                        }
                    } else {
                        csAssert.assertTrue(allTypeValues.contains("Attachment"), "Template Type Value is not Attachment in Downloaded Excel");
                    }
                } else {
                    csAssert.assertFalse(true, "Downloaded Excel doesn't contain Type Column.");
                }

                FileUtils.deleteFile(filePath, fileName);
            } else {
                csAssert.assertFalse(true, "Couldn't Download Excel file for Contract Document Tab Data of CDR Id " + cdrId +
                        ". Hence Couldn't validate further");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C42017. " + e.getMessage());
        }
    }

    /*
    TC-C46354: Verify that via Upload - Change in Template Type updates all corresponding version in Cluster in Contracts.
     */
    @Test
    public void testC46354() {
        CustomAssert csAssert = new CustomAssert();
        int contractId = createContract();

        try {
            logger.info("Starting Test TC-C46354: Verify that via Upload - Change in Template Type updates all corresponding version in Cluster in Contracts.");
            if (contractId == -1) {
                throw new Exception("Couldn't Create Contract. Hence couldn't validate further.");
            }

            //Upload 1st Version with Type as Main Template
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(61, contractId, "Sample.txt", randomKeyForFileUpload);

            JSONObject jsonObj = new JSONObject(draftResponse);

            String payload = getPayloadForSubmitDraft(61, contractId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                    1001, 1);
            SubmitDraft.hitSubmitDraft("contracts", payload);

            //Upload 2nd Version with Type as Attachment
            randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            draftResponse = uploadDocument(61, contractId, "Sample.txt", randomKeyForFileUpload);

            jsonObj = new JSONObject(draftResponse);

            payload = getPayloadForSubmitDraft(61, contractId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                    1002, 2);
            SubmitDraft.hitSubmitDraft("contracts", payload);

            payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListDataResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, payload);

            JSONArray jsonArr = new JSONObject(tabListDataResponse).getJSONArray("data");
            String typeColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "type");

            for (int i = 0; i < jsonArr.length(); i++) {
                String value = jsonArr.getJSONObject(i).getJSONObject(typeColumn).getString("value");

                if (!value.equalsIgnoreCase("Attachment")) {
                    String versionColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "version");

                    csAssert.assertFalse(true, "Template Type Validation failed for Document having Version " +
                            jsonArr.getJSONObject(i).getJSONObject(versionColumn).getString("value") + ". Expected Value: Attachment and Actual Value: " + value);
                }
            }

            //Validate Audit log
            validateAuditLog(61, contractId, "TC-C46354", csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C46354. " + e.getMessage());
        } finally {
            if (contractId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C46353: Verify that Template type is editable in Contract Document Tab of Contract which is linked to CDR.
     */
    @Test
    public void testC46353() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;
        int contractId = -1;

        try {
            logger.info("Starting Test TC-C46353: Verify that Template type is editable in Contract Document Tab of Contract which is linked to CDR.");
            cdrId = createCDR();

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence Couldn't Validate further.");
            }

            String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[1024],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + cdrId +
                    "],\"entityTypeId\":160},\"actualParentEntity\":{\"entityIds\":[1024],\"entityTypeId\":1}}";
            String contractCreateResponse = createContractFromCDRResponse(newPayload);
            String status = ParseJsonResponse.getStatusFromResponse(contractCreateResponse);

            if (!status.equalsIgnoreCase("success")) {
                throw new Exception("Couldn't Create Contract due to " + status + ". Hence Couldn't validate further.");
            }

            contractId = CreateEntity.getNewEntityId(contractCreateResponse);

            //Upload document on CDR Contract Document Tab.
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(160, cdrId, "Sample.txt", randomKeyForFileUpload);

            JSONObject jsonObj = new JSONObject(draftResponse);

            String payload = getPayloadForSubmitDraft(160, cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                    1001, 2);
            submitDraftObj.hitSubmitDraft(payload);

            payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            String tabListDataResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, payload);

            jsonObj = new JSONObject(tabListDataResponse).getJSONArray("data").getJSONObject(0);

            String documentNameColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "documentname");
            String documentFileId = jsonObj.getJSONObject(documentNameColumn).getString("value").split(":;")[4];

            //Move Document to Contract via Move to Tree
            String tabPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                    "\"filterJson\":{}},\"defaultParameters\":{\"targetEntityTypeId\":61,\"targetEntityId\":" + contractId +
                    ",\"docFlowType\":\"moveToTree\",\"baseEntityId\":" + contractId + ",\"baseEntityTypeId\":61}}";
            tabListDataResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 409, tabPayload);

            jsonObj = new JSONObject(tabListDataResponse);

            if (jsonObj.getJSONArray("data").length() == 0) {
                csAssert.assertFalse(true, "Documents not Available in Contract - Contract Document Tab for Move To Tree.");
            } else {
                //Move to Tree
                payload = "{\"baseEntityId\":" + contractId + ",\"baseEntityTypeId\":61,\"sourceEntityTypeId\":160,\"sourceEntityId\":" + cdrId +
                        ",\"entityTypeId\":61,\"entityId\":" + contractId + ",\"auditLogDocTreeFlowDocs\":[{\"auditLogDocFileId\":\"" + documentFileId + "\"}]," +
                        "\"sourceTabId\":2,\"statusId\":1}";

                UserTasksHelper.removeAllTasks();
                MoveToTreeSave saveObj = new MoveToTreeSave();
                saveObj.hitMoveToTreeSave(payload);

                String moveToTreeResponse = saveObj.getMoveToTreeSaveJsonStr();
                jsonObj = new JSONObject(moveToTreeResponse);
                if (jsonObj.has("success") && jsonObj.getBoolean("success")) {
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();

                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), null);
                    Map<String, String> moveToTreeJob = UserTasksHelper.waitForScheduler(300000L, 10000L, newTaskId);

                    if (moveToTreeJob.get("jobPassed").trim().equalsIgnoreCase("true")) {
                        //Validate document moved successfully.
                        payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
                        tabListDataResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, payload);

                        jsonObj = new JSONObject(tabListDataResponse);

                        if (jsonObj.getJSONArray("data").length() > 0) {
                            documentNameColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "documentname");
                            documentFileId = jsonObj.getJSONArray("data").getJSONObject(0)
                                    .getJSONObject(documentNameColumn).getString("value").split(":;")[4];

                            if (updateRecord("contracts", contractId, documentFileId)) {
                                //Validate Updated Template Type Value.
                                tabListDataResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, payload);

                                jsonObj = new JSONObject(tabListDataResponse);
                                String typeColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "type");

                                String value = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(typeColumn).getString("value");
                                if (!value.equalsIgnoreCase("attachment")) {
                                    csAssert.assertFalse(true, "Template Type Update Validation failed under Contract Document Tab of Contract. " +
                                            "Expected Value: Attachment and Actual Value: " + value);
                                }
                            } else {
                                csAssert.assertFalse(true, "Couldn't Update Template Type under Contract Document Tab of Contract.");
                            }
                        } else {
                            csAssert.assertFalse(true, "Document not visible under Contract Document Tab of Contract Id " + contractId +
                                    " after Move to Tree Job.");
                        }
                    } else if (moveToTreeJob.get("jobPassed").trim().equalsIgnoreCase("skip")) {
                        csAssert.assertFalse(true, "Move to Tree Job didn't finish in specified time limit.");
                    } else {
                        csAssert.assertFalse(true, "Move to Tree Job failed.");
                    }
                } else {
                    csAssert.assertFalse(true, "Move to Tree API failed.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C46353. " + e.getMessage());
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

    private String uploadDocument(int entityTypeId, int recordId, String fileName, String randomKeyForFileUpload) {
        FileUploadDraft fileUploadDraft = new FileUploadDraft();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("name", fileName.split("\\.")[0]);
        queryParameters.put("extension", fileName.split("\\.")[1]);
        queryParameters.put("entityTypeId", String.valueOf(entityTypeId));
        queryParameters.put("entityId", String.valueOf(recordId));
        queryParameters.put("key", randomKeyForFileUpload);

        return fileUploadDraft.hitFileUpload(configFilePath, fileName, queryParameters);
    }

    private String getPayloadForSubmitDraft(int entityTypeId, int recordId, String documentFileId, String documentKey, int templateTypeId, int documentStatusId) {
        String payload = null;

        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(entityTypeId, recordId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":" + templateTypeId + ",\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":" + documentStatusId +
                        ",\"permissions\":{\"financial\":false," + "\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false," +
                        "\"shareWithSupplierFlag\":false}]}";

                commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

                jsonData.put("comment", commentJsonObj);

                JSONObject finalPayload = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("data", jsonData);
                finalPayload.put("body", body);

                payload = finalPayload.toString();
            }
        } catch (Exception e) {
            logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
        }

        return payload;
    }

    private boolean updateRecord(String entityName, int recordId, String documentFileId) {
        try {
            String editGetResponse = editObj.hitEdit(entityName, recordId);
            JSONObject jsonData = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data");
            JSONObject commentJsonObj = jsonData.getJSONObject("comment");

            String commentDocumentsPayload = "{\"name\":\"commentDocuments\",\"multiEntitySupport\":false,\"values\":[{\"shareWithSupplierFlag\":false," +
                    "\"editableDocumentType\":true,\"editable\":true,\"templateTypeId\":1002,\"documentStatus\":{\"id\":2,\"name\":\"Final\"}," +
                    "\"documentFileId\":" + documentFileId + ",\"legal\":true,\"financial\":true}]}";
            commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

            jsonData.put("comment", commentJsonObj);

            JSONObject finalPayload = new JSONObject();
            JSONObject body = new JSONObject();
            body.put("data", jsonData);
            finalPayload.put("body", body);

            String editPostResponse = editObj.hitEdit(entityName, finalPayload.toString());
            return ParseJsonResponse.getStatusFromResponse(editPostResponse).equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }

    //TC-C42085: Verify Audit Log captures all information.
    private void validateAuditLog(int entityTypeId, int recordId, String additionalInfo, CustomAssert csAssert) {
        try {
            logger.info("Validating Audit Log for EntityTypeId {} and Record Id {}. [{}]", entityTypeId, recordId, additionalInfo);
            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(entityTypeId, recordId, 61, payload);

            JSONObject jsonObj = new JSONObject(tabListResponse).getJSONArray("data").getJSONObject(0);
            String actionNameColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "action_name");

            if (!jsonObj.getJSONObject(actionNameColumn).getString("value").equalsIgnoreCase("Document Uploaded")) {
                csAssert.assertFalse(true, "Audit Log Validation failed for EntityTypeId " + entityTypeId + " and Record Id " +
                        recordId + ". [" + additionalInfo + "]. Expected Action Name: Document Uploaded and Actual Action Name: " +
                        jsonObj.getJSONObject(actionNameColumn).getString("value"));
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Audit Log for Record Id " + recordId + " of EntityTypeId " + entityTypeId +
                    ". [" + additionalInfo + "]. " + e.getMessage());
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
}