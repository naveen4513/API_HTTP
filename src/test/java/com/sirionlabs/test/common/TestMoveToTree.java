package com.sirionlabs.test.common;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Comment;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.moveToTree.GetCRIdsForContract;
import com.sirionlabs.api.moveToTree.Save;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.ChangeRequest;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.test.filters.TestFilters;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TestMoveToTree {

    private final static Logger logger = LoggerFactory.getLogger(TestFilters.class);

    private String filePath = "src/test/resources/TestConfig";
    private TabListData tabListObj = new TabListData();
    private int docId = -1;

    @Test
    public void testDocumentMoveToTreeFromCRToContract() {
        CustomAssert csAssert = new CustomAssert();
        Integer contractId = -1;
        Integer crId = -1;

        try {
            contractId = getContractId();
            if (contractId == null) {
                throw new SkipException("Couldn't create Contract.");
            }

            crId = getCRId(contractId);
            if (crId == null) {
                throw new SkipException("Couldn't create Change Request");
            }

            validateGetCRIdsAPIResponse(contractId, crId, false, csAssert);

            moveCRToApprovedState(crId);

            logger.info("Validating Document Move to Tree from CR to Contract for Contract Id {} and CR Id {}", contractId, crId);

            validateGetCRIdsAPIResponse(contractId, crId, true, csAssert);

            String documentName = getDocumentName();
            renameUploadFile(documentName);

            uploadDocumentFileToCR(documentName, crId);

            revertUploadFileName(documentName);

            validateUploadedDocumentInCRCommunicationTab(documentName, crId);

            String payloadForCRDocIdInContract = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                    "\"filterJson\":{}}," + "\"defaultParameters\":{\"targetEntityTypeId\":61,\"targetEntityId\":" + contractId +
                    ",\"docFlowType\":\"moveToTree\",\"baseEntityId\":" + contractId + ",\"baseEntityTypeId\":61}}";

            validateDocumentComingInContractTabListResponse(documentName, contractId, crId, payloadForCRDocIdInContract);

            UserTasksHelper.removeAllTasks();

            validateMoveToTreeDocument(documentName, contractId, crId);

            validateEntryInContractAuditLog(documentName, contractId, csAssert);

            validateDocumentAfterMovingToTree(documentName, crId, payloadForCRDocIdInContract);
        } catch (SkipException e) {
            csAssert.assertTrue(false, e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Document Move to Tree from CR to Contract. " + e.getMessage());
        } finally {
            if (contractId != null)
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);

            if (crId != null)
                EntityOperationsHelper.deleteEntityRecord("change requests", crId);
        }
        csAssert.assertAll();
    }


    //This method covers TC-C46284 and TC-C46388
    private void validateGetCRIdsAPIResponse(int contractId, int crId, boolean approvedFlag, CustomAssert csAssert) {
        try {
            GetCRIdsForContract getCRIdObj = new GetCRIdsForContract();
            String getCRIdResponse = getCRIdObj.hitGetCRIdsForContract(contractId);
            Show showObj = new Show();

            if (ParseJsonResponse.validJsonResponse(getCRIdResponse)) {
                logger.info("Validating that GetCRIdsForContract contains CR Id {} or not.", crId);
                JSONObject getCRIdsJsonObj = new JSONObject(getCRIdResponse);
                JSONArray jsonArr = getCRIdsJsonObj.getJSONArray("response");
                boolean crRecordFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    if (jsonArr.getJSONObject(i).getInt("id") == crId) {
                        crRecordFound = true;
                        break;
                    }
                }

                if (!crRecordFound) {
                    if (approvedFlag)
                        throw new SkipException("CR having Id " + crId + " not found in GetCRIdsForContract API Response.");
                }

                logger.info("Validating that all CRs returned in GetCRIds API Response are created from Contract having Id {} only.", contractId);
                for (int i = 0; i < jsonArr.length(); i++) {
                    int crEntityId = jsonArr.getJSONObject(i).getInt("id");
                    showObj.hitShow(63, crEntityId);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        JSONObject showPageJsonObj = new JSONObject(showResponse);
                        showPageJsonObj = showPageJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("parentEntityId");
                        int parentEntityId = showPageJsonObj.getInt("values");

                        if (parentEntityId != contractId) {
                            throw new SkipException("Expected Parent Entity Id: " + contractId + " and Actual Parent Entity Id: " + parentEntityId +
                                    " of CR Id: " + crEntityId);
                        }
                    } else {
                        throw new SkipException("Show API Response for CR Id " + crEntityId + " is an Invalid JSON.");
                    }
                }
            } else {
                throw new SkipException("GetCRIdsForContract API Response is an Invalid JSON for Contract Id " + contractId);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating GetCRIds API Response. " + e.getMessage());
        }
    }

    private String getDocumentName() {
        String currTime = Long.toString(System.currentTimeMillis());
        currTime = currTime.substring(currTime.length() - 5, currTime.length());
        return "Test Move To Tree" + currTime + ".txt";
    }

    private void renameUploadFile(String documentName) {
        File fileToUpload = new File(filePath + "/Test Move To Tree.txt");
        fileToUpload.renameTo(new File(filePath + "/" + documentName));
    }

    private void revertUploadFileName(String documentName) {
        File fileToUpload = new File(filePath + "/" + documentName);
        fileToUpload.renameTo(new File(filePath + "/Test Move To Tree.txt"));
    }

    private void uploadDocumentFileToCR(String documentName, int crId) {
        try {
            logger.info("Uploading document {} to CR.", documentName);
            String randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);
            String documentUploadResponse = DocumentHelper.uploadDocumentFile(filePath, documentName, randomKeyForDocumentFile);

            if (documentUploadResponse == null || !documentUploadResponse.contains(documentName)) {
                throw new SkipException("File Upload Response doesn't contain Document Name in it. Document Name: [" + documentName + "] and Response: [" +
                        documentUploadResponse + "]");
            }

            String commentFieldPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null}," +
                    "\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409},\"comments\":{\"name\":\"comments\",\"id\":86,\"values\":\"\"}," +
                    "\"draft\":{\"name\":\"draft\"},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243}," +
                    "\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"values\":false}," +
                    "\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null}," +
                    "\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247}," +
                    "\"commentDocuments\":{\"values\":[{\"key\":\"" + randomKeyForDocumentFile + "\",\"performanceData\":false,\"searchable\":false," +
                    "\"legal\":false,\"financial\":false,\"businessCase\":false}]}}";

            Show showObj = new Show();
            showObj.hitShow(63, crId);
            String showResponse = showObj.getShowJsonStr();
            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
            jsonObj = jsonObj.put("comment", new JSONObject(commentFieldPayload));
            String payloadForCommentAPI = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";

            Comment commentObj = new Comment();
            String commentResponse = commentObj.hitComment("change requests", payloadForCommentAPI);
            if (!ParseJsonResponse.successfulResponse(commentResponse)) {
                throw new SkipException("Comment API Response is not successful.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            throw new SkipException("Exception while Uploading Document " + documentName + " to CR Id " + crId);
        }
    }

    private void validateUploadedDocumentInCRCommunicationTab(String documentName, int crId) {
        try {
            logger.info("Validating that Newly Uploaded Document is Available in TabListData API Response.");
            String crCommunicationTabResponse = tabListObj.hitTabListData(65, 63, crId);

            if (ParseJsonResponse.validJsonResponse(crCommunicationTabResponse)) {
                JSONObject crComJsonObj = new JSONObject(crCommunicationTabResponse);
                JSONArray crComJsonArr = crComJsonObj.getJSONArray("data");

                boolean documentFoundInCrCommunicationTab = false;
                int columnId = ListDataHelper.getColumnIdFromColumnName(crCommunicationTabResponse, "document");

                String expectedDocumentName = FileUtils.getFileNameWithoutExtension(documentName);
                String expectedDocumentExtension = FileUtils.getFileExtension(documentName);

                for (int i = 0; i < crComJsonArr.length(); i++) {
                    String value = crComJsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnId)).getString("value");

                    if (value.contains(expectedDocumentName + ":;" + expectedDocumentExtension)) {
                        documentFoundInCrCommunicationTab = true;

                        String[] temp2 = value.split(Pattern.quote(":;"));
                        docId = Integer.parseInt(temp2[4]);

                        break;
                    }
                }

                if (!documentFoundInCrCommunicationTab) {
                    throw new SkipException("Document " + documentName + " not found in Communication Tab of CR Id " + crId);
                }
            } else {
                throw new SkipException("TabListData API Response for Communication Tab of CR Id " + crId + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            throw new SkipException("Exception while Validating Uploaded Document " + documentName + " in Communication Tab of CR Id " + crId);
        }
    }

    //This method covers TC-C46389, TC-C46391
    private void validateDocumentComingInContractTabListResponse(String documentName, int contractId, int crId, String payloadForCRDocIdInContract) {
        try {
            String tabListResponse = tabListObj.hitTabListData(409, 63, crId, payloadForCRDocIdInContract);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject tabListJsonObj = new JSONObject(tabListResponse);
                JSONArray tabListJsonArr = tabListJsonObj.getJSONArray("data");

                int columnIdForDocumentName = ListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");
                int columnIdForUploadedOn = ListDataHelper.getColumnIdFromColumnName(tabListResponse, "uploadedon");
                int columnIdForSource = ListDataHelper.getColumnIdFromColumnName(tabListResponse, "source");

                boolean documentFoundInTabListResponse = false;

                for (int i = 0; i < tabListJsonArr.length(); i++) {
                    tabListJsonObj = tabListJsonArr.getJSONObject(i);

                    if (tabListJsonObj.getJSONObject(String.valueOf(columnIdForDocumentName)).getString("value").equalsIgnoreCase(documentName)) {
                        documentFoundInTabListResponse = true;

                        if (tabListJsonObj.getJSONObject(String.valueOf(columnIdForUploadedOn)).isNull("value") ||
                                tabListJsonObj.getJSONObject(String.valueOf(columnIdForUploadedOn)).getString("value").trim().equalsIgnoreCase("")) {
                            throw new SkipException("UploadedOn Column Validation failed in Contract Tab List Response.");
                        }

                        if (!tabListJsonObj.getJSONObject(String.valueOf(columnIdForSource)).getString("value").equalsIgnoreCase("Communication")) {
                            throw new SkipException("Source Column Validation failed in Contract Tab List Response. Expected Value: [Communication] and Actual Value: [" +
                                    tabListJsonObj.getJSONObject(String.valueOf(columnIdForSource)).getString("value") + "]");
                        }

                        break;
                    }
                }

                if (!documentFoundInTabListResponse) {
                    throw new SkipException("Document not found in TabListData API Response for TabId 409 and CR Id " + crId + " of Contract Id " + contractId);
                }
            } else {
                throw new SkipException("TabListData API Response for Tab Id 409, CR Id " + crId + " of Contract Id " + contractId + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            throw new SkipException("Exception while Checking if Document is coming in Contract TabList Data Response. " + e.getMessage());
        }
    }


    //This method covers TC-C46392
    private void validateMoveToTreeDocument(String documentName, int contractId, int crId) {
        try {
            String payloadForMoveToTree = "{\"baseEntityId\":" + contractId + ",\"baseEntityTypeId\":61,\"sourceEntityTypeId\":63,\"sourceEntityId\":" + crId +
                    ",\"entityTypeId\":61,\"entityId\":" + contractId + ",\"auditLogDocTreeFlowDocs\":[{\"auditLogDocFileId\":\"" + docId +
                    "\"}],\"sourceTabId\":2,\"statusId\":1}";

            Save moveToTreeSaveObj = new Save();
            String moveToTreeResponse = moveToTreeSaveObj.hitSave(payloadForMoveToTree);

            if (ParseJsonResponse.validJsonResponse(moveToTreeResponse)) {
                String response = new JSONObject(moveToTreeResponse).getString("response");

                if (response.trim().equalsIgnoreCase("Your request has been successfully submitted.")) {
                    logger.info("Waiting for Scheduler to finish Move To Tree Job");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();
                    String fetchResponse = fetchObj.getFetchJsonStr();
                    Integer taskId = UserTasksHelper.getTaskIdFromDescription(fetchResponse, "Move to tree");

                    if (taskId == null) {
                        throw new SkipException("Couldn't get Task Id for Move To Tree.");
                    }

                    String jobResult = UserTasksHelper.waitForScheduler(120000L, 10000L, taskId).get("jobPassed");

                    if (jobResult.equalsIgnoreCase("skip")) {
                        throw new SkipException("Scheduler Job didn't finish in time.");
                    }

                    if (jobResult.equalsIgnoreCase("true")) {
                        logger.info("Validating that Document has been moved and is visible under Contract Tree");
                        ContractTreeData treeDataObj = new ContractTreeData();
                        HashMap<String, String> params = new HashMap<>();
                        params.put("hierarchy", "false");
                        treeDataObj.hitContractTreeDataListAPI(61, contractId, params);
                        String treeDataResponse = treeDataObj.getResponseContractTreeData();

                        if (ParseJsonResponse.validJsonResponse(treeDataResponse)) {
                            JSONObject treeJsonObj = new JSONObject(treeDataResponse);
                            treeJsonObj = treeJsonObj.getJSONObject("body").getJSONObject("data");

                            JSONArray childrenJsonArr = treeJsonObj.getJSONArray("children");

                            boolean documentFoundInTree = false;

                            for (int i = 0; i < childrenJsonArr.length(); i++) {
                                String actualDocumentName = childrenJsonArr.getJSONObject(i).getString("text");
                                String expectedDocumentName = FileUtils.getFileNameWithoutExtension(documentName);

                                if (actualDocumentName.trim().equalsIgnoreCase(expectedDocumentName)) {
                                    documentFoundInTree = true;
                                    break;
                                }
                            }

                            if (!documentFoundInTree) {
                                throw new SkipException("Document having Name " + documentName + " not found in Tree of Contract " + contractId);
                            }
                        } else {
                            throw new SkipException("ContractTree API Response is an Invalid JSON for Contract Id " + contractId);
                        }
                    } else {
                        throw new SkipException("Scheduler Job failed.");
                    }
                } else {
                    throw new SkipException("MoveToTree Request Failed.");
                }
            } else {
                throw new SkipException("MoveToTree API Response is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            throw new SkipException("Exception while Validating Move To Tree Document " + e.getMessage());
        }
    }

    private void validateEntryInContractAuditLog(String documentName, int contractId, CustomAssert csAssert) {
        try {
            String payloadForContractAuditLog = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":2,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String contractAuditLogResponse = tabListObj.hitTabListData(61, 61, contractId, payloadForContractAuditLog);

            JSONObject auditLogJsonObj = new JSONObject(contractAuditLogResponse);
            JSONArray auditLogJsonArr = auditLogJsonObj.getJSONArray("data");

            int columnIdForDocumentInAuditLogResponse = ListDataHelper.getColumnIdFromColumnName(contractAuditLogResponse, "document");
            int columnIdForHistoryInAuditLogResponse = ListDataHelper.getColumnIdFromColumnName(contractAuditLogResponse, "history");
            boolean entryFoundInAuditLog = false;

            for (int i = 0; i < auditLogJsonArr.length(); i++) {
                String documentValue = auditLogJsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnIdForDocumentInAuditLogResponse))
                        .getString("value");

                if (documentValue.equalsIgnoreCase("no")) {
                    entryFoundInAuditLog = true;

                    String historyValue = auditLogJsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnIdForHistoryInAuditLogResponse))
                            .getString("value");

                    String[] historyValueArr = historyValue.split(Pattern.quote("/"));
                    Long historyId = Long.parseLong(historyValueArr[3]);
                    FieldHistory historyObj = new FieldHistory();
                    String fieldHistoryResponse = historyObj.hitFieldHistory(historyId, 61);

                    JSONObject historyJsonObj = new JSONObject(fieldHistoryResponse);
                    historyJsonObj = historyJsonObj.getJSONArray("value").getJSONObject(0);

                    String newHistoryValue = historyJsonObj.getString("newValue");
                    String propertyValue = historyJsonObj.getString("property");
                    String stateValue = historyJsonObj.getString("state");

                    csAssert.assertTrue(newHistoryValue.equalsIgnoreCase("[" + documentName + "]"), "Expected New Value in History: "
                            + documentName + " and Actual Value: " + newHistoryValue);
                    csAssert.assertTrue(propertyValue.equalsIgnoreCase("Contract Document"),
                            "Expected Property Value in History: Contract Document and Actual Value: " + propertyValue);
                    csAssert.assertTrue(stateValue.equalsIgnoreCase("ADDED"),
                            "Expected State Value in History: ADDED and Actual Value: " + stateValue);

                    break;
                }
            }

            csAssert.assertTrue(entryFoundInAuditLog, "Entry not found in Audit Log.");

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            throw new SkipException("Exception while Validating Entry in Audit Logs of Contract Id " + contractId + ". " + e.getMessage());
        }
    }

    //This method covers TC-C46390
    private void validateDocumentAfterMovingToTree(String documentName, int crId, String payloadForCRDocIdInContract) {
        try {
            logger.info("Validating that After Moving Document to Tree it is no longer coming in TabListData API Response");
            String tabListResponse = tabListObj.hitTabListData(409, 63, crId, payloadForCRDocIdInContract);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject tabListJsonObj = new JSONObject(tabListResponse);
                JSONArray tabListJsonArr = tabListJsonObj.getJSONArray("data");

                boolean documentFoundInTabListResponse = false;
                int columnIdForDocumentName = ListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");

                for (int i = 0; i < tabListJsonArr.length(); i++) {
                    if (tabListJsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnIdForDocumentName))
                            .getString("value").equalsIgnoreCase(documentName)) {
                        documentFoundInTabListResponse = true;
                        break;
                    }
                }

                if (documentFoundInTabListResponse) {
                    throw new SkipException("Document Found in TabListData API Response for TabId 409 and CR Id " + crId);
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            throw new SkipException("Exception while Validating that Document is no longer coming in TabListData API Response after Moving to Tree. " + e.getMessage());
        }
    }

    private Integer getContractId() {
        try {
            String createResponse = Contract.createContract("contract for move to tree", true);

            if (ParseJsonResponse.validJsonResponse(createResponse) && ParseJsonResponse.successfulResponse(createResponse)) {
                return CreateEntity.getNewEntityId(createResponse, "contracts");
            }
        } catch (Exception e) {
            throw new SkipException("Exception while Creating Contract. " + e.getMessage());
        }

        return null;
    }

    private Integer getCRId(Integer contractId) {
        try {
            UpdateFile.updateConfigFileProperty("src/test/resources/Helper/EntityCreation/ChangeRequest", "changeRequest.cfg",
                    "cr for move to tree", "sourceid", contractId.toString());

            String createResponse = ChangeRequest.createChangeRequest("cr for move to tree", true);

            UpdateFile.updateConfigFileProperty("src/test/resources/Helper/EntityCreation/ChangeRequest", "changeRequest.cfg",
                    "cr for move to tree", "sourceid", "sourceid");

            if (ParseJsonResponse.validJsonResponse(createResponse) && ParseJsonResponse.successfulResponse(createResponse)) {
                return CreateEntity.getNewEntityId(createResponse, "change requests");
            }
        } catch (Exception e) {
            throw new SkipException("Exception while Creating CR. " + e.getMessage());
        }

        return null;
    }

    private void moveCRToApprovedState(Integer crId) {
        try {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName("change requests");

            hitWorkflowAPI("Submit", crId, entityTypeId);
            hitWorkflowAPI("Approve", crId, entityTypeId);
        } catch (Exception e) {
            throw new SkipException("Exception while moving CR to Approved State. " + e.getMessage());
        }
    }

    private void hitWorkflowAPI(String actionName, Integer crId, int entityTypeId) {
        try {
            Show showObj = new Show();
            showObj.hitShow(entityTypeId, crId);
            String showResponse = showObj.getShowJsonStr();

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                WorkflowActionsHelper workflowHelperObj = new WorkflowActionsHelper();
                Boolean actionPerformed = workflowHelperObj.performWorkflowAction(showResponse, entityTypeId, crId, actionName);

                if (actionPerformed == null || !actionPerformed) {
                    throw new SkipException("Couldn't Perform Workflow Action " + actionName + " on CR Id " + crId);
                }
            }
        } catch (Exception e) {
            throw new SkipException("Exception while Performing Workflow Action " + actionName + " on CR Id " + crId + ". " + e.getMessage());
        }
    }
}