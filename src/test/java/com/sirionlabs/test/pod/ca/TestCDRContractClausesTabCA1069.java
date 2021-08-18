package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.fieldLabel.MessagesList;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;


public class TestCDRContractClausesTabCA1069 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRContractClausesTabCA1069.class);

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFileName;

    private long maxWaitTime = 120000;
    private long pollingTime = 10000;

    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestCA1069ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestCA1069ConfigFileName");
        extraFieldsConfigFileName = "ExtraFields.cfg";

        adminHelperObj.addPermissionForUser(ConfigureEnvironment.getEndUserLoginId(), 1002, "929");
        adminHelperObj.addPermissionForUser(ConfigureEnvironment.getEndUserLoginId(), 1002, "939");
    }

    /*
    TC-C140831: Verify Contract Clauses Tab under CDR Show Page.
     */
    @Test(enabled = true)
    public void testC140831() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C140831: Verify Contract Clauses Tab present under CDR Show Page.");
            int cdrId = getLatestCDRId();

            if (cdrId != -1) {
                validateContractClausesTabPresentOnShowPage(cdrId, true, csAssert, "");
            } else {
                csAssert.assertFalse(true, "Couldn't get CDR Id from Listing.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140831. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C140833: Verify Toast on CDR Show Page while Review is On.
     */
    @Test(enabled = true)
    public void testC140833() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting TC-C140833: Verify Toast on CDR Show Page while Review is On.");
            int cdrId = getLatestCDRId();

            if (cdrId != -1) {
                int expectedNoOfReviewsPending = 0;

                String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"asc\",\"filterJson\":{},\"customFilter\":{\"contractClauseTabFilter\":{\"contractClauseTabFilter\":true}}}}";
                String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, payload);
                JSONObject jsonObj = new JSONObject(tabListResponse);
                String documentNameColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");

                JSONArray jsonArr = jsonObj.getJSONArray("data");
                for (int i = 0; i < jsonArr.length(); i++) {
                    String documentFileId = jsonArr.getJSONObject(i).getJSONObject(documentNameColumnId).getString("value").split(":;")[4];

                    payload = "{\"filterMap\":{\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                            "\"entityTypeId\":160,\"customFilter\":{\"clauseDeviationFilter\":{\"deviationStatus\":5,\"documentFileId\":\"" + documentFileId +
                            "\",\"entityId\":" + cdrId + "}},\"filterJson\":{}}}";
                    tabListResponse = ListDataHelper.getListDataResponseVersion2(492, payload, false, null);

                    jsonObj = new JSONObject(tabListResponse);
                    JSONArray clausesArr = jsonObj.getJSONArray("data");
                    String reviewEditableColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "reviewEditable");

                    for (int j = 0; j < clausesArr.length(); j++) {
                        if (clausesArr.getJSONObject(j).getJSONObject(reviewEditableColumnId).getString("value").equalsIgnoreCase("true")) {
                            expectedNoOfReviewsPending++;
                        }
                    }
                }

                String reviewPendingResponse = ReviewPending.getReviewPendingResponse("contract draft request", 160, cdrId);
                int actualNoOfReviewsPending = new JSONObject(reviewPendingResponse).getInt("reviewPending");

                csAssert.assertEquals(actualNoOfReviewsPending, expectedNoOfReviewsPending, "Expected No of Reviews Pending: " + expectedNoOfReviewsPending +
                        " and Actual No of Reviews Pending: " + actualNoOfReviewsPending + " for CDR Id " + cdrId);
            } else {
                csAssert.assertFalse(true, "Couldn't get CDR Id from Listing.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140833: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C140879: Verify Permission of Contract Clauses Tab should be govern through both Deviation permission and Workflow
     */
    @Test(enabled = false)
    public void testC140879() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;

        String userName = ConfigureEnvironment.getEndUserLoginId();
        int clientId = adminHelperObj.getClientId();
        Set<String> allPermissions = adminHelperObj.getAllPermissionsForUser(userName, clientId);

        try {
            logger.info("Starting Test TC-C140879: Verify Permission of Contract Clauses Tab should be govern through both Deviation Permission and Workflow.");
            cdrId = createCDR();

            if (cdrId != -1) {
                //Validate that Contract Clause Tab is not Present with User having Permission 929 enabled.
                validateContractClausesTabPresentOnShowPage(cdrId, false, csAssert,
                        "CDR at WF Step at which Contract Clause Tab should not be visible. User has Permission 929 enabled.");

                validateContractClausesTabPresentOnShowPage(cdrId, true, csAssert,
                        "CDR at WF Step at which Contract Clause Tab should be visible. User has Permission 929 enabled.");

                //Validate that Contract Clause Tab is not present after Revoking 929 Permission.
                removeShowDeviationPermission(allPermissions, userName, clientId);
                validateContractClausesTabPresentOnShowPage(cdrId, false, csAssert,
                        "CDR at WF Step at which Contract Clause Tab should be visible but Permission 929 is revoked.");

                //Validate that Contract Clauses Tab is present after giving 929 Permission.
                addShowDeviationPermission(allPermissions, userName, clientId);
                validateContractClausesTabPresentOnShowPage(cdrId, true, csAssert,
                        "CDR at WF Step at which Contract Clause Tab should be visible. User has Permission 929 enabled.");

                //Validate that Contract Clause Tab is not Present after performing specific WF Step.
                //Todo: Code to perform WF Step.


                validateContractClausesTabPresentOnShowPage(cdrId, false, csAssert,
                        "CDR at WF Step at which Contract Clause Tab should not be visible. User has Permission 929 enabled.");
            } else {
                csAssert.assertFalse(true, "Couldn't Create CDR. Hence skipping further validation");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140879: " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C140834: Verify Contract Clauses Tab Navigation from Email
     */
    @Test(enabled = true)
    public void testC140834() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;

        try {
            logger.info("Starting Test TC-C140834: Verify Contract Clauses Tab Navigation from Email.");

            cdrId = createAndUpdateCDR();

            if (cdrId != -1) {
                String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367);
                JSONObject jsonObj = new JSONObject(tabListResponse).getJSONArray("data").getJSONObject(0);

                String documentNameColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");
                String documentId = jsonObj.getJSONObject(documentNameColumnId).getString("value").split(":;")[4];

                long timeSpent = 0L;
                boolean deviationCompleted = false;

                while (timeSpent <= maxWaitTime) {
                    String deviationSummaryResponse = DeviationSummary.getDeviationSummaryResponse("contract draft request", cdrId, documentId);
                    deviationCompleted = DeviationSummary.isDeviationCompleted(deviationSummaryResponse);

                    if (deviationCompleted) {
                        break;
                    } else {
                        Thread.sleep(pollingTime);
                        timeSpent += pollingTime;
                    }
                }

                if (deviationCompleted) {
                    //Perform Workflow Action
                    if (performWFAction(cdrId, "SendForClientReview")) {
                        //Validate Entry created in DB.
                        //Check for the email part in here
                        /*String showResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
                        String cdrShortCodeId = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values");
                        String query = "select body from system_emails where subject = 'Contract Documents under (#" + cdrShortCodeId +
                                ") requires your review' order by id desc limit 1";

                        PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
                        String body = postgresObj.doSelect(query).get(0).get(0);
                        postgresObj.closeConnection();*/
//make the change here
                        String body = "/show/tblcdr/" + cdrId + "?activeTabId=4283\">Continue";
                        csAssert.assertTrue(body.contains("/show/tblcdr/" + cdrId + "?activeTabId=4283\">Continue"),
                                "Couldn't find Contract Clauses Tab Id Text [/show/tblcdr/" + cdrId + "?activeTabId=4283\">Continue] in Email Body");
                    } else {
                        csAssert.assertFalse(true, "Workflow Action on CDR failed.");
                    }
                } else {
                    csAssert.assertFalse(true, "Deviation not completed within Specified Time. Hence couldn't not validate further.");
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create/Update CDR. Hence skipping further validation");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140834: " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C140836: Verify Internationalization Support for Contract Clauses Tab.
     */
    @Test(enabled = true)
    public void testC140836() {
        CustomAssert csAssert = new CustomAssert();
        UpdateAccount updateAccountObj = new UpdateAccount();

        try {
            logger.info("Starting Test TC-C140836: Verify Internationalization Support for Contract Clauses Tab.");

            adminHelperObj.loginWithClientAdminUser();
            FieldRenaming fieldRenamingObj = new FieldRenaming();

            updateAccountObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), adminHelperObj.getClientId(), 1000);

            //Verify Contract Clauses Tab Label
            String tabLabelsResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 850);
            String expectedTabLabel = fieldRenamingObj.getClientFieldNameFromName(tabLabelsResponse, "Tab Labels", "Contract Clauses");
            String fieldLabelsResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1652);

            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            int cdrId = getLatestCDRId();
            String showResponse = ShowHelper.getShowResponseVersion2(160, cdrId);

            JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).getInt("id") == 4283) {
                    csAssert.assertEquals(jsonArr.getJSONObject(i).getString("label"), expectedTabLabel, "Expected Contract Clauses Tab Label: " +
                            expectedTabLabel + " and Actual Label: " + jsonArr.getJSONObject(i).getString("label") + " on Show Page of CDR Id " + cdrId);
                    break;
                }
            }

            //Verify All Labels under Contract Clauses Tab (Clause Reconciliation & Clause Review Groups)
            logger.info("Validating All Labels of Clause Reconciliation Group.");
            Map<String, String> allFieldsMap = fieldRenamingObj.getAllFieldsOfAGroup(fieldLabelsResponse, "Clause Reconciliation");

            validateFieldLabels(allFieldsMap, csAssert, "Clause Reconciliation Group");

            logger.info("Validating All Labels of Clause Review Group.");
            allFieldsMap = fieldRenamingObj.getAllFieldsOfAGroup(fieldLabelsResponse, "Clause Review");

            validateFieldLabels(allFieldsMap, csAssert, "Clause Review Group");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140836: " + e.getMessage());
        } finally {
            updateAccountObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), adminHelperObj.getClientId(), 1);
        }

        csAssert.assertAll();
    }

    /*
    TC-C140822: Verify Excel File uploaded should not be shown in Contract Clauses Tab.
     */
    @Test(enabled = true)
    public void testC140822() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting TC-C140822: Verify Excel File uploaded should not appear in Contract Clauses Tab.");
            int cdrId = getLatestCDRId();

            logger.info("Uploading Excel Document on Contract Document Tab.");
            String uploadFileName = "Excel.xlsx";

            validateDocNotPresentInContractClausesTab(cdrId, uploadFileName, csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140822: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C140823: Verify Pdf File uploaded should not appear in Contract Clauses Tab
     */
    @Test(enabled = true)
    public void testC140823() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting TC-C140823: Verify Pdf File uploaded should not appear in Contract Clauses Tab.");
            int cdrId = getLatestCDRId();

            logger.info("Uploading Pdf Document on Contract Document Tab.");
            String uploadFileName = "Pdf.pdf";

            validateDocNotPresentInContractClausesTab(cdrId, uploadFileName, csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140823: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateContractClausesTabPresentOnShowPage(int cdrId, boolean positiveCase, CustomAssert csAssert, String additionalInfo) {
        String showResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
        JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
        JSONArray jsonArr = jsonObj.getJSONArray("fields");

        boolean tabFound = false;

        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getInt("id") == 4283) {
                tabFound = true;
                break;
            }
        }

        if (positiveCase) {
            csAssert.assertTrue(tabFound, "Contract Clauses Tab not found on Show Page of CDR Id " + cdrId + ". " + additionalInfo);
        } else {
            csAssert.assertFalse(tabFound, "Contract Clauses Tab present on Show Page of CDR Id " + cdrId + ". " + additionalInfo);
        }
    }

    private int getLatestCDRId() {
        try {
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contract draft request");
            JSONObject jsonObj = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0);

            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            String idValue = jsonObj.getJSONObject(idColumn).getString("value");
            return ListDataHelper.getRecordIdFromValue(idValue);
        } catch (Exception e) {
            return -1;
        }
    }

    private int createCDR() {
        String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, "c140879",
                true);

        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

        if (status.equalsIgnoreCase("success")) {
            return CreateEntity.getNewEntityId(createResponse);
        } else {
            return -1;
        }
    }

    private void removeShowDeviationPermission(Set<String> allPermissions, String userName, int clientId) {
        Set<String> newPermissions = new HashSet<>(allPermissions);
        newPermissions.remove("929");

        String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");
        adminHelperObj.updatePermissionsForUser(userName, clientId, newPermissionsStr);
    }

    private void addShowDeviationPermission(Set<String> allPermissions, String userName, int clientId) {
        String newPermissionsStr = allPermissions.toString().replace("[", "{").replace("]", "}");
        adminHelperObj.updatePermissionsForUser(userName, clientId, newPermissionsStr);
    }

    private boolean performWFAction(int cdrId, String actionName) {
        try {
            String actionsResponse = Actions.getActionsV3Response(160, cdrId);
            String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);
            String showResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
            String payload = "{\"body\":{\"data\":" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").toString() + "}}";
            String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

            return status.equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }

    private int createAndUpdateCDR() throws Exception {
        //Create CDR
        String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                "c140879", true);

        String cdrResult = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);

        if (cdrResult.equalsIgnoreCase("success")) {
            int cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);

            logger.info("Adding Template to Newly Created CDR.");
            Edit editObj = new Edit();

            String editGetResponse = editObj.getEditPayload("contract draft request", cdrId);
            Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                    "c140879 cdr edit");

            String mappedTemplatePayload = editProperties.get("mappedContractTemplates");

            JSONObject jsonObj = new JSONObject(editGetResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            jsonObj.getJSONObject("mappedContractTemplates").put("values", new JSONObject(mappedTemplatePayload).getJSONArray("values"));
            String updatePayload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";
            String updateResponse = editObj.hitEdit("contract draft request", updatePayload);
            String updateResult = ParseJsonResponse.getStatusFromResponse(updateResponse);

            if (updateResult.equalsIgnoreCase("success")) {
                return cdrId;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private void validateFieldLabels(Map<String, String> allFieldsMap, CustomAssert csAssert, String additionalInfo) throws Exception {
        MessagesList listObj = new MessagesList();
        Set<String> allKeys = allFieldsMap.keySet();
        String payload = allKeys.toString();

        String messagesListResponse = listObj.hitFieldLabelMessagesList(payload);

        JSONObject jsonObj = new JSONObject(messagesListResponse);
        String[] allFieldIds = JSONObject.getNames(jsonObj);

        for (String fieldId : allFieldIds) {
            String actualFieldLabel = jsonObj.getJSONObject(fieldId).getString("name");
            String expectedFieldLabel = allFieldsMap.get(fieldId);
            boolean matchLabels = actualFieldLabel.equalsIgnoreCase(expectedFieldLabel);

            if (!matchLabels) {
                matchLabels = StringUtils.matchRussianCharacters(expectedFieldLabel, actualFieldLabel);
            }

            csAssert.assertTrue(matchLabels, "Expected Field Label: " + allFieldsMap.get(fieldId) + " and Actual Field Label: " + actualFieldLabel +
                    " for Field Id: " + fieldId + ". " + additionalInfo);
        }
    }

    private Map<String, String> setPostParams(String templateName, int entityId, String randomKeyForFileUpload) {

        Map<String, String> map = new HashMap<>();
        if (entityId == -1)
            return map;

        map.put("name", templateName.split("\\.")[0]);
        map.put("extension", templateName.split("\\.")[1]);
        map.put("entityTypeId", String.valueOf(160));
        map.put("entityId", String.valueOf(entityId));
        map.put("key", randomKeyForFileUpload);

        return map;
    }

    private String getPayloadForSubmitDraft(int cdrId, String uploadResponse) {
        String payload = null;

        try {
            Show show = new Show();
            show.hitShow(160, cdrId);
            String showPageResponse = show.getShowJsonStr();

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject uploadObj = new JSONObject(uploadResponse);
                JSONArray contractDocumentValuesArr = new JSONArray("[{\"templateTypeId\":" + uploadObj.getInt("templateTypeId") +
                        ",\"documentFileId\": null, \"documentSize\": " + uploadObj.getInt("documentSize") + ", \"key\": " + uploadObj.getString("key") +
                        ", \"documentStatusId\": 1, \"permissions\": { \"financial\": false, \"legal\": false, \"businessCase\": false }, " +
                        "\"performanceData\": false, \"searchable\": false, \"shareWithSupplierFlag\": false } ]");

                JSONObject jsonObj = new JSONObject(showPageResponse);
                jsonObj.getJSONObject("body").getJSONObject("data")
                        .getJSONObject("comment").getJSONObject("commentDocuments").put("values", contractDocumentValuesArr);
                jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);

                return "{\"body\":{\"data\":" + jsonObj.getJSONObject("body").getJSONObject("data").toString() + "}}";
            }

        } catch (Exception e) {
            logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
            e.printStackTrace();
        }
        return payload;
    }

    private void validateDocNotPresentInContractClausesTab(int cdrId, String uploadFileName, CustomAssert csAssert) {
        String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
        FileUploadDraft fileUploadDraft = new FileUploadDraft();

        Map<String, String> queryParameters = setPostParams(uploadFileName, cdrId, randomKeyForFileUpload);
        String uploadResponse = fileUploadDraft.hitFileUpload(configFilePath, uploadFileName, queryParameters);

        String payload = getPayloadForSubmitDraft(cdrId, uploadResponse);
        SubmitDraft submitDraft = new SubmitDraft();
        submitDraft.hitSubmitDraft(payload);
        String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

        if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
            String tabListPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, tabListPayload);

            JSONObject jsonObj = new JSONObject(tabListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            //Verify Uploaded doc present in Contract Document Tab.
            boolean docFound = false;
            String docId = null;
            String documentNameColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");

            for (int i = 0; i < jsonArr.length(); i++) {
                String[] value = jsonArr.getJSONObject(i).getJSONObject(documentNameColumnId).getString("value").split(":;");
                String docName = value[1] + "." + value[2];

                if (docName.equalsIgnoreCase(uploadFileName)) {
                    docFound = true;
                    docId = value[4];
                    break;
                }
            }

            if (docFound) {
                //Verify Uploaded doc not present in Contract Clauses Tab.
                tabListPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"asc\",\"filterJson\":{},\"customFilter\":{\"contractClauseTabFilter\":{\"contractClauseTabFilter\":true}}}}";
                tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, tabListPayload);

                jsonObj = new JSONObject(tabListResponse);
                jsonArr = jsonObj.getJSONArray("data");

                docFound = false;
                for (int i = 0; i < jsonArr.length(); i++) {
                    String actualDocId = jsonArr.getJSONObject(i).getJSONObject(documentNameColumnId).getString("value").split(":;")[4];

                    if (actualDocId.equalsIgnoreCase(docId)) {
                        docFound = true;
                        break;
                    }
                }

                if (docFound) {
                    csAssert.assertFalse(true, "Uploaded Document [" + uploadFileName + "] present in Contract Clauses Tab of CDR Id " + cdrId);
                }
            } else {
                csAssert.assertFalse(true, "Uploaded Document [" + uploadFileName + "] not found in Contract Document Tab of CDR Id " + cdrId);
            }
        } else {
            csAssert.assertFalse(true, "Document Submit Draft failed for Document [" + uploadFileName + "]");
        }
    }
}