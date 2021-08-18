package com.sirionlabs.test.cdr;


import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.commonAPI.*;

import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.cdr.CdrHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestCDRFlowVPC extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRFlowVPC.class);

    private String configFilePath;
    private String configFileName;

    private String extraFieldsConfigFilePath;
    private String extraFieldsConfigFileName;
    int newClauseId;
    int newTemplateId;
    int newCdrId;

    int cdrEntityTypeId = 160;
    int clauseEntityTypeId = 138;
    int contractTemplateEntityTypeId = 140;

    int cdrIdForClone;
    int templateIdForClone;
    int clauseIdForClone;

    int clauseCategory;
    List<String> wFStepsToPublishTemplate;

    private long maxWaitTime = 300000;
    private long pollingTime = 10000;

    String contractDraftRequest = "contract draft request";
    String contractTemplate = "contract templates";
    String clause = "clauses";

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRCreationTestConfigFileName");

        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"extrafieldsconfigfilepath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"extrafieldsconfigfilename");

        wFStepsToPublishTemplate = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"wf steps to publish template").split("->"));

        cdrIdForClone = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity id for cloning","contract draft request"));
        clauseIdForClone = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity id for cloning","clauses"));
        templateIdForClone = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity id for cloning","contract templates"));

        clauseCategory = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"clause category"));
    }

    @Test(enabled = true)
    public void TestClauseCreation(){

        CustomAssert customAssert = new CustomAssert();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        try{

            Clone clone = new Clone();
            String clause = "clauses";

            String cloneResponse = clone.hitCloneV2(clause,clauseIdForClone);

            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();
            createEntityBodyJson.put("data", dataJson);
            createEntityJson.put("body", createEntityBodyJson);

            Create create = new Create();
            create.hitCreate(clause, createEntityJson.toString());

            String createResponse = create.getCreateJsonStr();

            newClauseId = CreateEntity.getNewEntityId(createResponse, clause);

            String wf_steps_to_publish_clause = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"wf steps to publish clause");

            if(wf_steps_to_publish_clause != null){
                if(!wf_steps_to_publish_clause.equals("")){

                    List<String> wf_steps_to_publish_clause_List = Arrays.asList(wf_steps_to_publish_clause.split("->"));

                    workflowActionsHelper.performWorkFlowStepsV2(clauseEntityTypeId,newClauseId,wf_steps_to_publish_clause_List,customAssert);

                }
            }

            System.out.println();
        }catch (Exception e){
            logger.error("Exception while validating TestClauseCreation");
            customAssert.assertTrue(false,"Exception while validating TestClauseCreation");

        }

        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestClauseCreation")
    public void TestTemplateCreation(){

        CustomAssert customAssert = new CustomAssert();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        try{
            Clone clone = new Clone();
            String template = "contract templates";

            String cloneResponse = clone.hitClone(template,templateIdForClone);

            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();

            JSONObject clauseJson = new JSONObject();

//            clauseJson.put("clauseCategory",new JSONObject("{\"name\": \"Acceptance\",\"id\": " + 1001 + "}"));
            clauseJson.put("clauseCategory",new JSONObject("{\"name\": \"Acceptance\",\"id\": " + clauseCategory + "}"));
            clauseJson.put("clause",new JSONObject("{\"name\": \"Automation\",\"id\": " + newClauseId + "}"));
//            clauseJson.put("clause",new JSONObject("{\"name\": \"Automation\",\"id\": " + 1362 + "}"));
//            clauseJson.put("clauseGroup",new JSONObject("{\"name\": \"Clause\",\"id\": " + 1 + "}"));
            clauseJson.put("clauseGroup",new JSONObject("{\"name\": \"Clause\",\"id\": " + 2 + "}"));
            clauseJson.put("order",1);
            clauseJson.put("mandatory",JSONObject.NULL);

            dataJson.getJSONObject("clauses").getJSONArray("values").put(0,clauseJson);
            createEntityBodyJson.put("data", dataJson);

            createEntityJson.put("body", createEntityBodyJson);


            Create create = new Create();
            create.hitCreate(template, createEntityJson.toString());

            String createResponse = create.getCreateJsonStr();

            newTemplateId = CreateEntity.getNewEntityId(createResponse, template);

            workflowActionsHelper.workFlowStepsToPerform(contractTemplateEntityTypeId,newTemplateId,wFStepsToPublishTemplate,customAssert);

            Download download = new Download();

            String filePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"upload document path");
            String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"upload document name");

            String queryString = "/contracttemplate/download/" + newTemplateId + "?";

            download.hitDownload(filePath,fileName,queryString);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while creating template");
        }
        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestTemplateCreation")
    public void testCDRCreation() {
        CustomAssert customAssert = new CustomAssert();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        newCdrId = -1;
        try {

//            workflowActionsHelper.workFlowStepsToPerform(contractTemplateEntityTypeId,newTemplateId,wFStepsToPublishTemplate,customAssert);
            Clone clone = new Clone();
            String contractDraftRequest = "contract draft request";

            String cloneResponse = clone.hitCloneV2(contractDraftRequest,cdrIdForClone);

            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();
            createEntityBodyJson.put("data", dataJson);
            createEntityJson.put("body", createEntityBodyJson);

            Create create = new Create();
            create.hitCreate(contractDraftRequest, createEntityJson.toString());

            String createResponse = create.getCreateJsonStr();

            newCdrId = CreateEntity.getNewEntityId(createResponse, contractDraftRequest);

            CdrHelper cdrHelper = new CdrHelper();

            String filePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"upload document path");
            String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"upload document name");
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);

            String draftResponse = cdrHelper.uploadDocument(newCdrId,filePath,fileName,randomKeyForFileUpload);

            JSONObject jsonObj = new JSONObject(draftResponse);

            String payload = cdrHelper.getPayloadForSubmitFinal(newCdrId, jsonObj.get("documentFileId").toString(),randomKeyForFileUpload);
            SubmitDraft submitDraft = new SubmitDraft();

            submitDraft.hitSubmitDraft(payload);
            String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

            if (!ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                customAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
            }
            validateReviewProcess(newCdrId,customAssert);

            System.out.println("");
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating CDR Creation Flow" + e.getMessage());
        }
        customAssert.assertAll();
    }

    @AfterClass
    public void AfterClass(){

        EntityOperationsHelper.deleteEntityRecord(contractDraftRequest,newCdrId);
        EntityOperationsHelper.deleteEntityRecord(clause,newClauseId);
        EntityOperationsHelper.deleteEntityRecord(contractTemplate,newTemplateId);
    }

    private void validateReviewProcess(int cdrId,CustomAssert csAssert){

        try {
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
                    if (performActionOnClause(cdrId, "SendForClientReview")) {
                        String listDataPayload = getContractClausesPayload(documentId, cdrId, "5");

                        String listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
                        String reviewEditableColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "reviewEditable");
                        jsonObj = new JSONObject(listDataResponse);

                        if (jsonObj.getJSONArray("data").length() > 0) {
                            jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);

                            String reviewEditableValue = jsonObj.getJSONObject(reviewEditableColumnId).getString("value");

                            validateCancelReviewPositiveFlow(documentId, cdrId, csAssert);

                            if (reviewEditableValue.equalsIgnoreCase("true")) {
                                //Hit API to Cancel Review
                                String contentControlColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "contentControlId");
                                String contentControlIdValue = jsonObj.getJSONObject(contentControlColumnId).getString("value");
                                String cancelReviewPayload = "{\"contentControlId\":\"" + contentControlIdValue + "\",\"documentFileId\":\"" + documentId +
                                        "\",\"modifiedText\":null,\"reviewTaskDetail\":{\"taskId\":1002,\"clientId\":0,\"initialTask\":false,\"finalTask\":true," +
                                        "\"labelId\":29921,\"taskName\":\"Review Cancelled\",\"taskButtonName\":\"Cancel Review\",\"visibleOutward\":true," +
                                        "\"buttonLabelId\":30056,\"reviewPermission\":939}}";

                                int cancelReviewResponseCode = PerformTask.cancelReview("contract draft request", cancelReviewPayload);

                                if (cancelReviewResponseCode == 200) {
                                    //Verify Review Editable is False.
                                    listDataPayload = getContractClausesPayload(documentId, cdrId, "null");

                                    listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
                                    jsonObj = new JSONObject(listDataResponse);

                                    if (jsonObj.getJSONArray("data").length() > 0) {
                                        jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);

                                        reviewEditableValue = jsonObj.getJSONObject(reviewEditableColumnId).getString("value");

                                        if (reviewEditableValue.equalsIgnoreCase("true")) {
                                            csAssert.assertFalse(true, "Review Editable is still true in ListData 492 API Response.");
                                        }
                                    } else {
                                        csAssert.assertFalse(true, "No Record coming in ListData 492 API after Cancelling Review.");
                                    }
                                } else {
                                    csAssert.assertFalse(true, "Couldn't Cancel Review for Document Id " + documentId + " on CDR Contract Clauses Tab.");
                                }
                            } else {
                                csAssert.assertFalse(true, "Review Button not found in Contract Clauses Tab of CDR.");
                            }
                        } else {
                            csAssert.assertFalse(true, "Review button not coming up in Contract Clauses Tab of CDR.");
                        }
                    } else {
                        csAssert.assertFalse(true, "Workflow Action on CDR failed.");
                    }
                } else {
                    csAssert.assertFalse(true, "Deviation not completed within Specified Time. Hence couldn't not validate further.");
                }
            }
        }catch (Exception e){
            csAssert.assertTrue(false,"");
        }
    }

    private boolean performActionOnClause(int cdrId, String actionName) {
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

    private String getContractClausesPayload(String documentId, int cdrId, String deviationStatus) {
        return "{\"filterMap\":{\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                "\"entityTypeId\":160,\"customFilter\":{\"clauseDeviationFilter\":{\"deviationStatus\":" + deviationStatus + "," +
                "\"documentFileId\":\"" + documentId + "\",\"entityId\":" + cdrId + "}},\"filterJson\":{}}}";
    }

    private void validateCancelReviewPositiveFlow(String documentId, int cdrId, CustomAssert csAssert) {
        try {
            String listDataPayload = getContractClausesPayload(documentId, cdrId, "5");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
            JSONObject jsonObj = new JSONObject(listDataResponse);

            if (jsonObj.getJSONArray("data").length() > 0) {
                jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);
                String reviewTaskDetailColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "reviewTaskDetail");
                String reviewTaskDetailValue = jsonObj.getJSONObject(reviewTaskDetailColumnId).getString("value").replaceAll("\\\\", "");
                JSONObject reviewTaskJsonObj = new JSONObject(reviewTaskDetailValue).getJSONObject("currentTask");
                org.json.JSONArray jsonArr = reviewTaskJsonObj.getJSONArray("nextTaskIds");

                boolean cancelReviewButtonFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String taskButtonName = jsonArr.getJSONObject(i).getString("taskButtonName");

                    if (taskButtonName.equalsIgnoreCase("Cancel Review")) {
                        cancelReviewButtonFound = true;

                        if (!jsonArr.getJSONObject(i).getBoolean("visibleOutward")) {
                            csAssert.assertFalse(true,
                                    "VisibleOutward flag is off in ListData API Response for CDR Contract Clauses Tab. However Task is present.");
                        }

                        break;
                    }
                }

                if (!cancelReviewButtonFound) {
                    csAssert.assertFalse(true, "Cancel Review Button not found even after the End User having permission.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Positive Flow of Cancel Review Permission. " + e.getMessage());
        }
    }
}
