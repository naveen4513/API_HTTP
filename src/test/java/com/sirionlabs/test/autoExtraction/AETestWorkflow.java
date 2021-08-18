package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;

public class AETestWorkflow extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(AETestWorkflow.class);

    static int recordId, initialAuditLogCount;

    public static int auditLogTab() throws IOException
    {
        String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        HttpResponse auditLogTabResponse = AutoExtractionHelper.getAuditLog(payload,String.valueOf(recordId));
        String auditLogStr = EntityUtils.toString(auditLogTabResponse.getEntity());
        JSONObject jsonObj=new JSONObject(auditLogStr);
        int count=Integer.parseInt(jsonObj.get("filteredCount").toString());
        return count;
    }


    public boolean performAction(int recordId, String actionName) {
        try {
            CustomAssert csAssert = new CustomAssert();
            String actionsResponse = Actions.getActionsV3Response(316, recordId);
            String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);
            HttpResponse showResponse = AutoExtractionHelper.docShowAPI(recordId);
            csAssert.assertTrue(showResponse.getStatusLine().getStatusCode() == 200, "Response code is invalid");
            String showResponseStr = EntityUtils.toString(showResponse.getEntity());
            JSONObject showResponseJson = new JSONObject(showResponseStr);
            String payload = "{\"body\":{\"data\":" + showResponseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

            return status.equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void workFlowActionForAE() throws IOException {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Getting the recordId for Assigned Documents");
            HttpResponse assignedDocumentsResponse = AutoExtractionHelper.assignedFilter();
            csAssert.assertTrue(assignedDocumentsResponse.getStatusLine().getStatusCode()==200,"Response Code is invalid");
            String assignedDocumentsStr = EntityUtils.toString(assignedDocumentsResponse.getEntity());
            JSONObject assignedDocumentsJson = new JSONObject(assignedDocumentsStr);
            int columnId = ListDataHelper.getColumnIdFromColumnName(assignedDocumentsStr, "documentname");

                JSONObject documentObj = assignedDocumentsJson.getJSONArray("data").getJSONObject(0);
                String documentNameValue = documentObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] docId = documentNameValue.split(":;");
                recordId = (Integer.valueOf(docId[1]));

            try {
                logger.info("Hitting Doc Show API");
                HttpResponse showResponse = AutoExtractionHelper.docShowAPI(recordId);
                csAssert.assertTrue(showResponse.getStatusLine().getStatusCode() == 200, "Response code is invalid");
                String showResponseStr = EntityUtils.toString(showResponse.getEntity());
                JSONObject showResponseJson = new JSONObject(showResponseStr);

                if (ParseJsonResponse.validJsonResponse(showResponseStr)) {
                    logger.info("Hitting Layout Actions API to get the current workflow status of the Document");
                    HttpResponse layoutActionResponse = AutoExtractionHelper.getLayoutAction(recordId);
                    csAssert.assertTrue(layoutActionResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                    String layoutActionStr = EntityUtils.toString(layoutActionResponse.getEntity());
                    JSONObject layoutActionJson = new JSONObject(layoutActionStr);
                    String workflowActionName = String.valueOf(layoutActionJson.getJSONArray("layoutActions").getJSONObject(0).get("name"));
                    String[] actions = {workflowActionName};

                    //Initial Count of audit log
                    logger.info("Initial Count of Audit Log Tab");
                    initialAuditLogCount = auditLogTab();
                    logger.info("Initial Count in Audit Log:" + initialAuditLogCount);

                    //Perform Workflow Actions
                    logger.info("Performing workflow Action");
                    boolean actionPerformed = true;
                    String actionFailed = null;

                    for (String actionName : actions) {
                        actionPerformed = performAction(recordId, actionName);

                        if (!actionPerformed) {
                            actionFailed = actionName;
                            break;
                        }
                    }

                    if (!actionPerformed) {
                        csAssert.assertFalse(true, "Action " + actionFailed + " failed. Hence skipping further validation.");
                    }
                }
            }
            catch (Exception e)
            {
                logger.info("Exception occured while hitting Doc Show API of AE");
                csAssert.assertTrue(false, e.getMessage());
            }

        } catch (Exception e) {
            logger.info("Exception occured while applying Filters of Assigned Documents on AE listing");
            csAssert.assertTrue(false, e.getMessage());
        }
            csAssert.assertAll();
    }

    @Test(dependsOnMethods = "workFlowActionForAE")
    public void validateAuditLog() throws IOException
    {
        CustomAssert csAssert= new CustomAssert();
        try {
            logger.info("Checking Final Count of Audit Log tab after performing workflow Action");
            int finalAuditLogTabCount = auditLogTab();
            logger.info("Final Count of entries in audit log tab" + " " + finalAuditLogTabCount);
            csAssert.assertTrue(finalAuditLogTabCount > initialAuditLogCount, "After Performing workflow action it is not adding entry in Audit Log tab");
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting Audit Log API");
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }
}