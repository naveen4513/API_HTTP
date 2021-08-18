package com.sirionlabs.test.cdr;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestCDRMisc extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRMisc.class);

    private String configFilePath = "src/test/resources/TestConfig/PreSignature/CDRMisc";
    private String configFileName = "TestCDRMisc.cfg";

    private int cdrId = -1;
    private int contractId = -1;
    private String uploadFileName;
    private String uploadFiledoc;
    private String uploadFileExcel;
    private String uploadpng;
    private AdminHelper adminHelperObj = new AdminHelper();
    private FieldRenaming fieldRenamingObj = new FieldRenaming();
    private Edit editObj = new Edit();
    private SubmitDraft submitDraft = new SubmitDraft();
    private TabListData tabListData = new TabListData();

    @BeforeClass
    public void beforeClass() {
        createCDRAndContract();

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String currentDate = dateFormat.format(date);
        uploadFileName = "Sample_" + currentDate + ".txt";
        uploadFiledoc = "CdrContractDocTab.docx";
        uploadFileExcel = "MusterRoll.xlsx";
        uploadpng= "DBconnections.png";
        FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);
        FileUtils.copyFile(configFilePath, "CdrContractDocTab.docx", configFilePath, uploadFileName);
        FileUtils.copyFile(configFilePath, "MusterRoll.xlsx", configFilePath, uploadFileName);
        FileUtils.copyFile(configFilePath, "DBconnections.png", configFilePath, uploadFileName);
    }

    @AfterClass
    public void afterClass() {
        if (contractId != -1) {
//            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }

        if (cdrId != -1) {
//            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
        }
    }

    private void createCDRAndContract() {
        String sectionName = "cdr creation";
        String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                sectionName, false);
        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

        if (status.equalsIgnoreCase("success")) {
            cdrId = CreateEntity.getNewEntityId(createResponse);

            String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[1024],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + cdrId +
                    "],\"entityTypeId\":160},\"actualParentEntity\":{\"entityIds\":[1024],\"entityTypeId\":1}}";
            createResponse = createContractFromCDRResponse(newPayload);
            status = ParseJsonResponse.getStatusFromResponse(createResponse);

            if (status.equalsIgnoreCase("success")) {
                contractId = CreateEntity.getNewEntityId(createResponse);
            }
        }
    }

    /*
    TC-C4477: Verify the Source Type Options in CDR. Only MSA, SOW, Work Order and Other should come.
    TC-C4474: Verify that PSA Option is not coming in Source Type drop-down of CDR.
     */
    @Test
    public void testC4477() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating TC-C4477: Verify Source Type Options in CDR. Only MSA, SOW, Work Order and Other should come.");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contract draft request");
            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            String idValue = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0).getJSONObject(idColumn).getString("value");

            int cdrRecordId = ListDataHelper.getRecordIdFromValue(idValue);
            String showResponse = ShowHelper.getShowResponseVersion2(160, cdrRecordId);

            String showFieldHierarchy = ShowHelper.getShowFieldHierarchy("cdr source type options id", 160);
            List<String> allOptionIds = ShowHelper.getAllOptionsOfField(showResponse, showFieldHierarchy);

            if (allOptionIds == null || allOptionIds.isEmpty()) {
                csAssert.assertFalse(true, "Couldn't get All Source Type Options Id from Show Response of CDR Id " + cdrRecordId);
            } else {
                Integer[] expectedIds = {4, 76, 9, 10};
                List<Integer> allExpectedOptionIds = new ArrayList<>(Arrays.asList(expectedIds));

                for (String optionId : allOptionIds) {
                    if (!allExpectedOptionIds.contains(Integer.parseInt(optionId))) {
                        csAssert.assertFalse(true, "Source Type Option Id " + optionId + " is wrongly present in CDR.");
                    }
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4477: " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C4475: Verify Internationalization of CDR Fields (Supplier, Source Type, Source Name) on Show Page
     */
    @Test
    public void testC4475() {
        CustomAssert csAssert = new CustomAssert();
        UpdateAccount updateAccountObj = new UpdateAccount();
        String endUserLoginId = ConfigureEnvironment.getEndUserLoginId();
        int clientId = adminHelperObj.getClientId();

        try {
            logger.info("Changing User Language to Russian.");
            updateAccountObj.updateUserLanguage(endUserLoginId, clientId, 1000);

            //Validating Field Labels on CDR Show Page.
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contract draft request");
            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            String idValue = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0).getJSONObject(idColumn).getString("value");

            int cdrRecordId = ListDataHelper.getRecordIdFromValue(idValue);
            String showResponse = ShowHelper.getShowResponseVersion2(160, cdrRecordId);

            adminHelperObj.loginWithClientAdminUser();
            String fieldLabelResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 850);

            String[] allFields = {"Suppliers", "Source name/Title", "Source Type"};

            for (String field : allFields) {
                String expectedLabel = fieldRenamingObj.getClientFieldNameFromName(fieldLabelResponse, "Metadata", field);
                String fieldName = getFieldNameForLabel(field);
                Map<String, String> fieldMap = ParseJsonResponse.getFieldByName(showResponse, fieldName);

                String actualFieldLabel = fieldMap.get("label");
                matchLabels(expectedLabel, actualFieldLabel, csAssert, "Field Label Validation for Field " + field);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4475: " + e.getMessage());
        } finally {
            logger.info("Changing User Language back to English.");
            updateAccountObj.updateUserLanguage(endUserLoginId, clientId, 1);
            new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }

    private String getFieldNameForLabel(String fieldLabel) {
        switch (fieldLabel.toLowerCase()) {
            case "source name/title":
                return "contractParent";

            case "source type":
                return "contractParentEntityType";

            default:
                return fieldLabel;
        }
    }

    private void matchLabels(String expectedFieldLabel, String actualFieldLabel, CustomAssert csAssert, String additionalInfo) {
        boolean matchLabels = StringUtils.matchRussianCharacters(expectedFieldLabel, actualFieldLabel);
        csAssert.assertTrue(matchLabels, "Expected " + expectedFieldLabel + " Label: " + expectedFieldLabel + " and Actual Label: " + actualFieldLabel + ". " +
                additionalInfo);
    }

    /*
    TC-C13805: Verify CDR Clone functionality
     */
    @Test
    public void testC13805() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C13805: Verify CDR Clone functionality.");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contract draft request");
            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            JSONObject jsonObj = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0);

            String idValue = jsonObj.getJSONObject(idColumn).getString("value");
            int cdrId = ListDataHelper.getRecordIdFromValue(idValue);

            logger.info("Cloning CDR Id {}", cdrId);
            int newCdrId = EntityOperationsHelper.cloneRecord("contract draft request", cdrId);

            if (newCdrId == -1) {
                csAssert.assertFalse(true, "Couldn't Clone CDR Id " + cdrId);
            } else {
//                EntityOperationsHelper.deleteEntityRecord("contract draft request", newCdrId);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C13805: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C7541
     */
    @Test
    public void testC7541() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7541");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);
            validateDocumentStatusOptions(draftResponse, csAssert);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), 1, randomKeyForFileUpload);
                submitDraft.hitSubmitDraft(payload);
                String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

                if (!ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7541. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    @Test(dependsOnMethods = "testC7541")
    public void testC7542() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7542");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);
            validateDocumentStatusOptions(draftResponse, csAssert);

            //Validate C7543: Upload Final Doc
            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), 2, randomKeyForFileUpload);
                submitDraft.hitSubmitDraft(payload);
                String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    //Validate C7544
                    validateC7544(documentFileId, csAssert);
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7542: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateC7544(String documentFileId, CustomAssert csAssert) {
        try {
            logger.info("Starting Test TC-C7544.");
            String editGetResponse = editObj.hitEdit("contract draft request", cdrId);
            JSONObject jsonData = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data");
            JSONObject commentJsonObj = jsonData.getJSONObject("comment");

            String commentDocumentsPayload = "{\"name\":\"commentDocuments\",\"multiEntitySupport\":false,\"values\":[{\"shareWithSupplierFlag\":false," +
                    "\"editableDocumentType\":true,\"editable\":true,\"templateTypeId\":1001,\"documentStatus\":{\"id\":1,\"name\":\"Draft\"},\"documentFileId\":" +
                    documentFileId + "}]}";
            commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

            jsonData.put("comment", commentJsonObj);

            JSONObject finalPayload = new JSONObject();
            JSONObject body = new JSONObject();
            body.put("data", jsonData);
            finalPayload.put("body", body);
            String payload = finalPayload.toString();

            String editPostResponse = editObj.hitEdit("contract draft request", payload);
            String status = ParseJsonResponse.getStatusFromResponse(editPostResponse);
            if (!status.equalsIgnoreCase("success")) {
                csAssert.assertFalse(true, "Couldn't Change Document Status to Draft from Final due to " + status);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7544");
        }
    }


    @Test(dependsOnMethods = "testC7542")
    public void testC7545() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7545");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);
                String documentFileId = jsonObj.get("documentFileId").toString();

                String payload = getPayloadForSubmitDraft(cdrId, documentFileId, 2, randomKeyForFileUpload);
                submitDraft.hitSubmitDraft(payload);
                String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
                    draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

                    jsonObj = new JSONObject(draftResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("documentStatus");

                    if (jsonArr.length() == 2) {
                        //Validate 1st option is Final.
                        jsonObj = jsonArr.getJSONObject(0);
                        csAssert.assertTrue(jsonObj.getString("name").equalsIgnoreCase("Final") && jsonObj.getInt("id") == 2,
                                "Default Option is not Final while Uploading Document on CDR Contract Document Tab.");

                        //Validate 2nd option is Final.
                        jsonObj = jsonArr.getJSONObject(1);
                        csAssert.assertTrue(jsonObj.getString("name").equalsIgnoreCase("Executed") && jsonObj.getInt("id") == 3,
                                "Executed Option is not 2nd Option while Uploading Document on CDR Contract Document Tab.");

                        //Upload Document as Executed.
                        payload = getPayloadForSubmitDraft(cdrId, documentFileId, 3, randomKeyForFileUpload);
                        submitDraft.hitSubmitDraft(payload);
                        submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

                        if (!ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                            csAssert.assertFalse(true, "Marking Document as Executed failed while Uploading Document on CDR Contract Document Tab.");
                        }
                    } else {
                        csAssert.assertFalse(true,
                                "Options Available Validation failed while Uploading Document on CDR Contract Document Tab. Expected Options: Draft, Final");
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7545: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    @Test(dependsOnMethods = "testC7545")
    public void testC13796() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C13796");
            String documentFileId = getLatestDocumentId();

            String editGetResponse = editObj.hitEdit("contract draft request", cdrId);
            JSONObject jsonData = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data");
            JSONObject commentJsonObj = jsonData.getJSONObject("comment");

            String commentDocumentsPayload = "{\"name\":\"commentDocuments\",\"multiEntitySupport\":false,\"values\":[{\"shareWithSupplierFlag\":false," +
                    "\"editableDocumentType\":true,\"editable\":true,\"templateTypeId\":1001,\"documentStatus\":{\"id\":4,\"name\":\"Discarded\"},\"documentFileId\":" +
                    documentFileId + "}]}";
            commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

            jsonData.put("comment", commentJsonObj);

            JSONObject finalPayload = new JSONObject();
            JSONObject body = new JSONObject();
            body.put("data", jsonData);
            finalPayload.put("body", body);
            String payload = finalPayload.toString();

            String editPostResponse = editObj.hitEdit("contract draft request", payload);
            String status = ParseJsonResponse.getStatusFromResponse(editPostResponse);
            if (!status.equalsIgnoreCase("success")) {
                csAssert.assertFalse(true, "Couldn't Change Document Status to Draft from Final due to " + status);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C13796. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C13799
    TC-C13798
     */
    @Test(dependsOnMethods = "testC13796")
    public void testC13799() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C13799.");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);
                String documentFileId = jsonObj.get("documentFileId").toString();

                JSONArray jsonArr = jsonObj.getJSONArray("documentStatus");

                if (jsonArr.length() == 1) {
                    //Validate Only Option is Executed.
                    jsonObj = jsonArr.getJSONObject(0);
                    csAssert.assertTrue(jsonObj.getString("name").equalsIgnoreCase("Executed") && jsonObj.getInt("id") == 3,
                            "Default Option is not Executed while Uploading Document on CDR Contract Document Tab.");

                    //Upload Document as Executed.
                    String payload = getPayloadForSubmitDraft(cdrId, documentFileId, 3, randomKeyForFileUpload);
                    submitDraft.hitSubmitDraft("cdr", payload);
                    String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

                    if (!ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                        csAssert.assertFalse(true, "Marking Document as Executed failed while Uploading Document on CDR Contract Document Tab.");
                    }
                } else {
                    csAssert.assertFalse(true,
                            "Options Available Validation failed while Uploading Document on CDR Contract Document Tab. Expected Options: Draft, Final");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C13799. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C7547
    TC-C7548
     */
    @Test(dependsOnMethods = "testC13799")
    public void testC7547() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7547.");

            if (contractId != -1) {
                //Validate Move To Tree
                String tabPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                        "\"filterJson\":{}},\"defaultParameters\":{\"targetEntityTypeId\":61,\"targetEntityId\":" + contractId +
                        ",\"docFlowType\":\"moveToTree\",\"baseEntityId\":" + contractId + ",\"baseEntityTypeId\":61}}";
                String tabListDataResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 409, tabPayload);

                JSONObject jsonObj = new JSONObject(tabListDataResponse);

                if (jsonObj.getJSONArray("data").length() == 0) {
                    csAssert.assertFalse(true, "Documents not Available in Contract - Contract Document Tab for Move To Tree.");
                }

                //Validate Inherit Files
                tabPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                        "\"filterJson\":{}},\"defaultParameters\":{\"targetEntityTypeId\":61,\"targetEntityId\":" + contractId + ",\"docFlowType\":\"inherit\"}}";
                tabListDataResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 409, tabPayload);

                jsonObj = new JSONObject(tabListDataResponse);

                if (jsonObj.getJSONArray("data").length() == 0) {
                    csAssert.assertFalse(true, "Documents not Available in Contract - Contract Document Tab for Inherit File.");
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create Contract from CDR. Hence couldn't validate further.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7547. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    C88815 - Verify that a flag is added in workflow inside value update task, then only executed status documents will get auto discarded and a new version of pdf for same document will get auto uploaded inside cluster.
    C88819 - Verify behavior if uploaded document is docx having status discarded and flag is true from workflow
    */
    @Test
    public void testc88815() {
        CustomAssert csAssert = new CustomAssert();
        createCDRAndContract();
        try {
            logger.info("Starting Test TC-C88815");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFiledoc, randomKeyForFileUpload);

            //Validate C7543: Upload Final Doc CdrContractDocTab.docx
            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);
                String finalflag = "Final";
                Thread.sleep(1000);
                String payload = getPayloadForSubmitFinal(cdrId, jsonObj.get("documentFileId").toString(), 2, randomKeyForFileUpload);
                Thread.sleep(1000);
                String submitDraftResponse = submitDraft.hitSubmitDraft("contract draft request",payload);

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponseFinal = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponseFinal);
                    String actualVerison = (String) jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value");
                    if (actualVerison.equals("1.0")) {
                        logger.info("Same document upload changed the version successfully to 1.1 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.1 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
            try {
                // upload same doc "CdrContractDocTab.docx" with executed status
                String randomKeyForExecutedFileUpload = RandomString.getRandomAlphaNumericString(12);
                String executedDraftResponse = uploadDocument(cdrId, uploadFiledoc, randomKeyForExecutedFileUpload);

                    JSONObject executedjsonObj = new JSONObject(executedDraftResponse);
                    String executed = "Executed";
                    String executedPayload = getPayloadForSubmitExecuted(cdrId, executedjsonObj.get("documentFileId").toString(), 3, randomKeyForExecutedFileUpload);
                    submitDraft.hitSubmitDraft(executedPayload);
                    String executedSubmitDraftResponse = submitDraft.getSubmitDraftJsonStr();
                    if (ParseJsonResponse.getStatusFromResponse(executedSubmitDraftResponse).equalsIgnoreCase("success")) {
                        String documentFileId = getLatestDocumentId();

                        // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                        String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                        JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                        String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                        if (actualVerison.equals("1.1")) {
                            logger.info("Same document upload changed the version successfully to 1.1 in CDR Contract document tab");
                        } else {
                            csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.1 instead the version is: " + actualVerison);
                        }
                    } else {
                        csAssert.assertFalse(true, "Submit Draft API failed while Uploading same Document as executed on CDR Contract Document Tab.");
                    }

                try {
                    String[] actions = {"SendForClientReview"};
                    boolean actionPerformed = true;
                    String actionFailed = null;
                    for (String actionName : actions) {

                        actionPerformed = performAction(cdrId, actionName, 160);

                        if (!actionPerformed) {
                            actionFailed = actionName;
                            break;
                        }
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
                }
                // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                if (actualVerison.equals("1.2")) {
                    logger.info("When workflow action performed SendForClientReview changed the version successfully to 1.2 in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.2 instead the version is: " + actualVerison);
                }
                String fileExtension = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[2];
                String  filename= jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[1];
                 String actualFile = filename+"."+ fileExtension;

                if (actualFile.equals("CdrContractDocTab.pdf")) {
                    logger.info("Document version changed to 1.2 and .doc file converted to .pdf in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "After Action performed SendForClientReview CdrContractDocTab.pdf is not been created and CdrContractDocTab.docx is not in Discarded status" + actualFile);
                }

            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while submitting CDR document tab Final document because: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    C88818 - Verify behavior if uploaded document is txt having status executed and flag is true from workflow
   */
    @Test
    public void testc88818() {
        CustomAssert csAssert = new CustomAssert();
        createCDRAndContract();
        try {
            logger.info("Starting Test TC-C88818");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            //Validate C7543: Upload Final Doc sample.txt
            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);
                String finalflag = "Final";
                String payload = getPayloadForSubmitFinal(cdrId, jsonObj.get("documentFileId").toString(), 2, randomKeyForFileUpload);
                String submitDraftResponse = submitDraft.hitSubmitDraft("contract draft request",payload);

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponseFinal = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponseFinal);
                    String actualVerison = (String) jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value");
                    if (actualVerison.equals("1.0")) {
                        logger.info("Document uploaded version is 1.0 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit Document as Final on CDR Contract Document Tab, version is not 1.0, instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
            try {
                // upload same doc "sample.txt" with executed status
                String randomKeyForExecutedFileUpload = RandomString.getRandomAlphaNumericString(12);
                String executedDraftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForExecutedFileUpload);

                JSONObject executedjsonObj = new JSONObject(executedDraftResponse);
                String executed = "Executed";
                String executedPayload = getPayloadForSubmitExecuted(cdrId, executedjsonObj.get("documentFileId").toString(), 3, randomKeyForExecutedFileUpload);
                submitDraft.hitSubmitDraft(executedPayload);
                String executedSubmitDraftResponse = submitDraft.getSubmitDraftJsonStr();
                if (ParseJsonResponse.getStatusFromResponse(executedSubmitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                    String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                    if (actualVerison.equals("1.1")) {
                        logger.info("Same document upload changed the version successfully to 1.1 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.1 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading same Document as executed on CDR Contract Document Tab.");
                }

                try {
                    String[] actions = {"SendForClientReview"};
                    boolean actionPerformed = true;
                    String actionFailed = null;
                    for (String actionName : actions) {

                        actionPerformed = performAction(cdrId, actionName, 160);

                        if (!actionPerformed) {
                            actionFailed = actionName;
                            break;
                        }
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
                }
                // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                if (actualVerison.equals("1.1")) {
                    logger.info(" When workflow performed SendForClientReview doc is not converted to pdf and version is 1.1 in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "When workflow performed SendForClientReview on CDR Contract Document Tab, expected version is 1.1 instead the version is: " + actualVerison);
                }
                String fileExtension = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[2];
                String  filename= jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[1];
                String actualFile = filename+"."+ fileExtension;

                if (actualFile.equals(uploadFileName)) {
                    logger.info("Document version is 1.1 and .txt file not converted to .pdf in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "After Action performed SendForClientReview CdrContractDocTab.pdf is not been created and CdrContractDocTab.docx is not in Discarded status" + actualFile);
                }

            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while submitting CDR document tab Final document because: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
   C88820 - Verify behavior if uploaded document is excel having status as discarded and flag is true from workflow
  */
    @Test
    public void testc88820() {
        CustomAssert csAssert = new CustomAssert();
        createCDRAndContract();
        try {
            logger.info("Starting Test TC-C88820");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileExcel, randomKeyForFileUpload);

            //Validate C7543: Upload Final Doc MusterRoll.xlsx
            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);
                String finalflag = "Final";
                String payload = getPayloadForSubmitFinal(cdrId, jsonObj.get("documentFileId").toString(), 2, randomKeyForFileUpload);
                String submitDraftResponse = submitDraft.hitSubmitDraft("contract draft request",payload);

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponseFinal = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponseFinal);
                    String actualVerison = (String) jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value");
                    if (actualVerison.equals("1.0")) {
                        logger.info("Document uploaded as Final and version is 1.0 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit Document as final on CDR Contract Document Tab, version is not 1.0 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
            try {
                // upload same doc "MusterRoll.xlsx" with executed status
                String randomKeyForExecutedFileUpload = RandomString.getRandomAlphaNumericString(12);
                String executedDraftResponse = uploadDocument(cdrId, uploadFileExcel, randomKeyForExecutedFileUpload);

                JSONObject executedjsonObj = new JSONObject(executedDraftResponse);
                String executed = "Executed";
                String executedPayload = getPayloadForSubmitExecuted(cdrId, executedjsonObj.get("documentFileId").toString(), 3, randomKeyForExecutedFileUpload);
                submitDraft.hitSubmitDraft(executedPayload);
                String executedSubmitDraftResponse = submitDraft.getSubmitDraftJsonStr();
                if (ParseJsonResponse.getStatusFromResponse(executedSubmitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                    String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                    if (actualVerison.equals("1.1")) {
                        logger.info("Same document upload changed the version successfully to 1.1 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.1 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading same Document as executed on CDR Contract Document Tab.");
                }

                try {
                    String[] actions = {"SendForClientReview"};
                    boolean actionPerformed = true;
                    String actionFailed = null;
                    for (String actionName : actions) {

                        actionPerformed = performAction(cdrId, actionName, 160);

                        if (!actionPerformed) {
                            actionFailed = actionName;
                            break;
                        }
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
                }
                // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                if (actualVerison.equals("1.1")) {
                    logger.info(" When workflow performed SendForClientReview doc is not converted to pdf and version is 1.1 in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "When workflow performed SendForClientReview on CDR Contract Document Tab, expected version is 1.1 instead the version is: " + actualVerison);
                }
                String fileExtension = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[2];
                String  filename= jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[1];
                String actualFile = filename+"."+ fileExtension;

                if (actualFile.equals(uploadFileExcel)) {
                    logger.info("Document version is 1.1 and .xlsx file is notconverted to .pdf in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "After Action performed SendForClientReview CdrContractDocTab .pdf is not been created and MusterRoll.xlsx is not in Discarded status" + actualFile);
                }

            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while submitting CDR document tab Final document because: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
  C88821 - Verify behavior if uploaded document is png having status as discarded and flag is true from workflow.
 */
   @Test
    public void testc88821() {
        CustomAssert csAssert = new CustomAssert();
       createCDRAndContract();
        try {
            logger.info("Starting Test TC-C88821");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadpng, randomKeyForFileUpload);

            //Validate C7543: Upload Final Doc DBconnection.png
            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);
                String finalflag = "Final";
                String payload = getPayloadForSubmitFinal(cdrId, jsonObj.get("documentFileId").toString(), 2, randomKeyForFileUpload);
                String submitDraftResponse = submitDraft.hitSubmitDraft("contract draft request",payload);

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponseFinal = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponseFinal);
                    String actualVerison = (String) jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value");
                    if (actualVerison.equals("1.0")) {
                        logger.info("Document upload the version is 1.1 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version is not 1.0 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
            try {
                // upload same doc "DBconnection.png" with executed status
                String randomKeyForExecutedFileUpload = RandomString.getRandomAlphaNumericString(12);
                String executedDraftResponse = uploadDocument(cdrId, uploadpng, randomKeyForExecutedFileUpload);

                JSONObject executedjsonObj = new JSONObject(executedDraftResponse);
                String executed = "Executed";
                String executedPayload = getPayloadForSubmitExecuted(cdrId, executedjsonObj.get("documentFileId").toString(), 3, randomKeyForExecutedFileUpload);
                submitDraft.hitSubmitDraft(executedPayload);
                String executedSubmitDraftResponse = submitDraft.getSubmitDraftJsonStr();
                if (ParseJsonResponse.getStatusFromResponse(executedSubmitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                    String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                    if (actualVerison.equals("1.1")) {
                        logger.info("Same document upload changed the version successfully to 1.1 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.1 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading same Document as executed on CDR Contract Document Tab.");
                }

                try {
                    String[] actions = {"SendForClientReview"};
                    boolean actionPerformed = true;
                    String actionFailed = null;
                    for (String actionName : actions) {

                        actionPerformed = performAction(cdrId, actionName, 160);

                        if (!actionPerformed) {
                            actionFailed = actionName;
                            break;
                        }
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
                }
                // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                if (actualVerison.equals("1.1")) {
                    logger.info(" When workflow performed SendForClientReview doc is not converted to pdf and version is 1.1 in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "When workflow performed SendForClientReview on CDR Contract Document Tab, expected version is 1.1 instead the version is: " + actualVerison);
                }
                String fileExtension = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[2];
                String  filename= jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[1];
                String actualFile = filename+"."+ fileExtension;

                if (actualFile.equals(uploadpng)) {
                    logger.info("Document version is 1.1 and .png file is not converted to .pdf in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "After Action performed SendForClientReview CdrContractDocTab .pdf is not been created and DBConnection.png is not in Discarded status " + actualFile);
                }

            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while submitting CDR document tab Final document because: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
 C88817 - Verify that new version of pdf doc will be made only for executed document status making previous one discarded.
*/
    @Test
    public void testC88817() {
        CustomAssert csAssert = new CustomAssert();
        createCDRAndContract();
        try {
            logger.info("Starting Test TC-C88817");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFiledoc, randomKeyForFileUpload);

            //Validate C7543: Upload Final Doc CdrContractDocTab.docx
            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);
                String finalflag = "Final";
                String payload = getPayloadForSubmitFinal(cdrId, jsonObj.get("documentFileId").toString(), 2, randomKeyForFileUpload);
                String submitDraftResponse = submitDraft.hitSubmitDraft("contract draft request",payload);

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponseFinal = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponseFinal);
                    String actualVerison = (String) jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value");
                    if (actualVerison.equals("1.0")) {
                        logger.info("Document upload version is 1.0 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.1 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
            try {
                // upload same doc "CdrContractDocTab.docx" with executed status
                String randomKeyForExecutedFileUpload = RandomString.getRandomAlphaNumericString(12);
                String executedDraftResponse = uploadDocument(cdrId, uploadFiledoc, randomKeyForExecutedFileUpload);

                JSONObject executedjsonObj = new JSONObject(executedDraftResponse);
                String executed = "Executed";
                String executedPayload = getPayloadForSubmitFinal(cdrId, executedjsonObj.get("documentFileId").toString(), 2, randomKeyForExecutedFileUpload);
                submitDraft.hitSubmitDraft(executedPayload);
                String executedSubmitDraftResponse = submitDraft.getSubmitDraftJsonStr();
                if (ParseJsonResponse.getStatusFromResponse(executedSubmitDraftResponse).equalsIgnoreCase("success")) {
                    String documentFileId = getLatestDocumentId();

                    // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                    String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                    JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                    String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                    if (actualVerison.equals("1.1")) {
                        logger.info("Same document upload changed the version successfully to 1.1 in CDR Contract document tab");
                    } else {
                        csAssert.assertFalse(true, "Submit same Document as executed on CDR Contract Document Tab, version did not change to 1.1 instead the version is: " + actualVerison);
                    }
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading same Document as executed on CDR Contract Document Tab.");
                }

                try {
                    String[] actions = {"SendForClientReview"};
                    boolean actionPerformed = true;
                    String actionFailed = null;
                    for (String actionName : actions) {

                        actionPerformed = performAction(cdrId, actionName, 160);

                        if (!actionPerformed) {
                            actionFailed = actionName;
                            break;
                        }
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
                }
                // check version listRenderer/list/367/tablistdata/160/7606?version=2.0
                String tablistDataResponse = tabListData.hitTabListDataV2(367, 160, cdrId);
                JSONObject jsonObjtabListdata = new JSONObject(tablistDataResponse);
                String actualVerison = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14387").get("value").toString();

                if (actualVerison.equals("1.1")) {
                    logger.info(" When workflow performed SendForClientReview doc is not converted to pdf and version is 1.1 in CDR Contract document tab");
                } else {
                    csAssert.assertFalse(true, "When workflow performed SendForClientReview on CDR Contract Document Tab, expected version is 1.1 instead the version is: " + actualVerison);
                }
                String fileExtension = jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[2];
                String  filename= jsonObjtabListdata.getJSONArray("data").getJSONObject(0).getJSONObject("14388").get("value").toString().split(":;")[1];
                String actualFile = filename+"."+ fileExtension;

                if (actualFile.equals(uploadFiledoc)) {
                    logger.info("Document version is 1.1 and .doc file is not converted to .pdf in CDR Contract document tab as file was in final state");
                } else {
                    csAssert.assertFalse(true, "After Action performed SendForClientReview CdrContractDocTab .pdf is not been created and CdrContractDocTab.doc is not in Discarded status as file was in final status " + actualFile);
                }

            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while submitting CDR document tab Executed document because: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while submitting CDR document tab Final document because: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private String uploadDocument(int cdrId, String fileName, String randomKeyForFileUpload) {
        FileUploadDraft fileUploadDraft = new FileUploadDraft();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("name", fileName.split("\\.")[0]);
        queryParameters.put("extension", fileName.split("\\.")[1]);
        queryParameters.put("entityTypeId", "160");
        queryParameters.put("entityId", String.valueOf(cdrId));
        queryParameters.put("key", randomKeyForFileUpload);
        queryParameters.put("documentFileData", ("binary"));

        return fileUploadDraft.hitFileUpload(configFilePath, fileName, queryParameters);
    }

    private void validateDocumentStatusOptions(String draftResponse, CustomAssert csAssert) {
        try {
            JSONObject jsonObj = new JSONObject(draftResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("documentStatus");

            if (jsonArr.length() == 2) {
                //Validate 1st option is Draft.
                jsonObj = jsonArr.getJSONObject(0);
                csAssert.assertTrue(jsonObj.getString("name").equalsIgnoreCase("Draft") && jsonObj.getInt("id") == 1,
                        "Default Option is not Draft while Uploading Document on CDR Contract Document Tab.");

                //Validate 2nd option is Final.
                jsonObj = jsonArr.getJSONObject(1);
                csAssert.assertTrue(jsonObj.getString("name").equalsIgnoreCase("Final") && jsonObj.getInt("id") == 2,
                        "Final Option is not 2nd Option while Uploading Document on CDR Contract Document Tab.");
            } else {
                csAssert.assertFalse(true,
                        "Options Available Validation failed while Uploading Document on CDR Contract Document Tab. Expected Options: Draft, Final");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Document Status Options in Draft API Response. " + e.getMessage());
        }
    }

    private String getPayloadForSubmitDraft(int cdrId, String documentFileId, int documentStatusId, String documentKey) {
        String payload = null;

        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(160, cdrId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":" + documentStatusId +
                        ",\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false," +
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

    private String getPayloadForSubmitFinal(int cdrId, String documentFileId, int documentStatusId, String documentKey) {
        String payload = null;

        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(160, cdrId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":2," +
                        "\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false," +
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

    private String getPayloadForSubmitExecuted(int cdrId, String documentFileId, int documentStatusId, String documentKey) {
        String payload = null;

        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(160, cdrId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":3," +
                        "\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false," +
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

    private String getLatestDocumentId() {
        String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        String tabListDataResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, payload);

        JSONObject jsonObj = new JSONObject(tabListDataResponse).getJSONArray("data").getJSONObject(0);
        String documentNameColumn = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "documentname");

        return jsonObj.getJSONObject(documentNameColumn).getString("value").split(":;")[4];
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

    public boolean performAction(int entityCreatedId, String actionName, int entityTypeId) {
        try {
            String actionsResponse = Actions.getActionsV3Response(entityTypeId, entityCreatedId);
            String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);
            String showResponse = ShowHelper.getShowResponseVersion2(entityTypeId, entityCreatedId);
            String payload = "{\"body\":{\"data\":" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").toString() + "}}";

            String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

            return status.equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }
}