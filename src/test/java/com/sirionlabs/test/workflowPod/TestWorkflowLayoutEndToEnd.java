package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.workflowLayout.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.WorkflowLayoutDbHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestWorkflowLayoutEndToEnd {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowLayoutEndToEnd.class);
    private int newlyCreatedLayoutId=0;

    @BeforeClass
    public void beforeClass() {
        new AdminHelper().loginWithClientAdminUser();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            String query = "delete from work_flow_layout where name ilike '%API Automation%'";
            sqlObj.deleteDBEntry(query);
        } catch (Exception e) {
            logger.error("Exception while Deleting Layout Data from DB having Id "+ e.getMessage());
        }
        finally {
            sqlObj.closeConnection();
        }
    }

    @AfterClass
    public void afterClass() {
        new Check().hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    @Test
    public void testWorkflowLayoutEndToEnd() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Hitting Workflow Layout Create Form API.");
        String createFormResponse = WorkflowLayoutCreateForm.getCreateFormResponse(WorkflowLayoutCreateForm.getApiPath(),
                WorkflowLayoutCreateForm.getHeaders()).getResponseBody();

        if (ParseJsonResponse.validJsonResponse(createFormResponse)) {
            List<Integer> allEntityTypeIds = WorkflowLayoutCreateForm.getAllEntityTypeIdsFromResponse(createFormResponse);

            if (allEntityTypeIds == null || allEntityTypeIds.isEmpty()) {
                throw new SkipException("Couldn't get All EntityTypeIds from Workflow Layout Create Form API Response.");
            }

            int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allEntityTypeIds.size(), 5);

            for (int randomNo: randomNumbers) {
                String entityName = ConfigureConstantFields.getEntityNameById(allEntityTypeIds.get(randomNo));

                try {
                    logger.info("Validating Workflow Layout End to End flow for Entity {}", entityName);
                    logger.info("Hitting Workflow Layout CreateForm Layout Info API for Entity {}", entityName);

                    String layoutInfoResponse = WorkflowLayoutCreateFormLayoutInfo.getCreateFormLayoutInfoResponse(
                            WorkflowLayoutCreateFormLayoutInfo.getApiPath(allEntityTypeIds.get(randomNo)), WorkflowLayoutCreateFormLayoutInfo.getHeaders()).getResponseBody();

                    if (ParseJsonResponse.validJsonResponse(layoutInfoResponse)) {
                        //Create Workflow Layout
                        String layoutName = "API Automation Workflow Layout End to End " + entityName;
                        List<Map<String, String>> allOptionsOfEditableFieldsShowPage =
                                WorkflowLayoutCreateFormLayoutInfo.getAllOptionsOfEditableFieldsShowPage(layoutInfoResponse);

                        List<Map<String, String>> allOptionsOfEditableFieldsEditPage =
                                WorkflowLayoutCreateFormLayoutInfo.getAllOptionsOfEditableFieldsEditPage(layoutInfoResponse);

                        List<Map<String, String>> allOptionsOfEditPageTabs = WorkflowLayoutCreateFormLayoutInfo.getAllOptionsOfEditPageTabs(layoutInfoResponse);

                        List<Map<String, String>> allOptionsOfShowPageTabs = WorkflowLayoutCreateFormLayoutInfo.getAllOptionsOfShowPageTabs(layoutInfoResponse);

                        String editableFieldsShowPageValue = getRandomOptionsStr(allOptionsOfEditableFieldsShowPage);
                        String editableFieldsEditPageValue = getRandomOptionsStr(allOptionsOfEditableFieldsEditPage);
                        String editPageTabsValue = getRandomOptionsStr(allOptionsOfEditPageTabs);
                        String showPageTabsValue = getRandomOptionsStr(allOptionsOfShowPageTabs);

                        String layoutCreatePayload = WorkflowLayoutCreate.getPayload(layoutName, editableFieldsShowPageValue, editableFieldsEditPageValue, editPageTabsValue,
                                showPageTabsValue, allEntityTypeIds.get(randomNo));

                        String layoutCreateResponse = WorkflowLayoutCreate.getCreateResponse(WorkflowLayoutCreate.getApiPath(), WorkflowLayoutCreate.getHeaders(),
                                layoutCreatePayload).getResponseBody();

                        if (ParseJsonResponse.validJsonResponse(layoutCreateResponse)) {
                            JSONObject jsonObj = new JSONObject(layoutCreateResponse).getJSONObject("header").getJSONObject("response");
                            newlyCreatedLayoutId = jsonObj.getInt("entityId");

                            //Validate Listing
                            validateLayoutListing(newlyCreatedLayoutId, layoutName, entityName, csAssert);

                            //Validate Show
                            validateLayoutShow(newlyCreatedLayoutId, entityName, layoutName, csAssert);

                            //Validate Audit log
                            validateAuditLog(newlyCreatedLayoutId, entityName, "saved", csAssert);

                            //Validate Edit
                            validateLayoutEdit(newlyCreatedLayoutId, editableFieldsShowPageValue, editableFieldsEditPageValue, editPageTabsValue,
                                    showPageTabsValue, entityName, allEntityTypeIds.get(randomNo), csAssert);

                            //Delete Layout
                            logger.info("Deleting Workflow Layout having Id: {}", newlyCreatedLayoutId);
                        } else {
                            csAssert.assertTrue(false, "Workflow Layout Create API Response for Entity " + entityName + " is an Invalid JSON.");
                        }
                    } else {
                        csAssert.assertTrue(false, "Workflow Layout CreateForm Layout Info API Response for Entity " + entityName + " is an Invalid JSON.");
                        return;
                    }
                } catch (SkipException e) {
                    throw new SkipException(e.getMessage());
                } catch (Exception e) {
                    csAssert.assertTrue(false, "Exception while Validating Workflow Layout End to End Flow for Entity: " + entityName + ". " +
                            e.getMessage());
                }
                finally {
                    if (newlyCreatedLayoutId!=0)
                        WorkflowLayoutDbHelper.deleteLayoutDataInDb(newlyCreatedLayoutId);
                }
            }
        } else {
            csAssert.assertTrue(false, "Workflow Layout Create Form API Response is an Invalid JSON.");
        }

        csAssert.assertAll();
    }

    private String getRandomOptionsStr(List<Map<String, String>> allOptionsList) {
        String valueStr = "";

        int maxRandomOptions = 2;
        int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRange(0, allOptionsList.size(), maxRandomOptions);

        for (int randomNumber : randomNumbers) {
            valueStr = valueStr.concat(allOptionsList.get(randomNumber).get("id") + ",");
        }

        valueStr = valueStr.substring(0, valueStr.length() - 1);

        return valueStr;
    }

    private void validateLayoutListing(int layoutId, String layoutName, String entityName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Layout Listing.");
            ListRendererListData listDataObj = new ListRendererListData();
            listDataObj.hitListRendererListData(468, true, ListDataHelper.getPayloadForListData(329, 20, 0), null);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONArray jsonArr = new JSONObject(listDataResponse).getJSONArray("data");
                int idColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                boolean layoutIdFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumn)).getString("value");

                    if (value.contains(String.valueOf(layoutId))) {
                        layoutIdFound = true;

                        //Validate Layout Name in Listing
                        String nameColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "name");
                        String nameValue = jsonArr.getJSONObject(i).getJSONObject(nameColumnId).getString("value");

                        csAssert.assertTrue(nameValue.equalsIgnoreCase(layoutName), "Expected Layout Name: " + layoutName + " and Actual Layout Name: " +
                                nameValue + " in ListData Response for Entity " + entityName);
                        break;
                    }
                }

                csAssert.assertTrue(layoutIdFound, "Workflow Layout having Id: " + layoutId + " not found in Listing API Response for Entity " + entityName);
            } else {
                csAssert.assertTrue(false, "ListData API Response for Workflow Layout is an Invalid JSON for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Listing for Layout Id: " + layoutId + " for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateLayoutShow(int layoutId, String entityName, String expectedLayoutName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Layout Show for Entity {}", entityName);
            String showResponse = WorkflowLayoutShow.getWorkflowLayoutShowResponse(WorkflowLayoutShow.getApiPath(layoutId), WorkflowLayoutShow.getHeaders()).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

                //Validate Layout Name
                String actualLayoutName = jsonObj.getJSONObject("workflowLayoutGroup").getString("values");
                csAssert.assertTrue(actualLayoutName.equalsIgnoreCase(expectedLayoutName), "Expected Layout Name: " + expectedLayoutName +
                        " and Actual Layout Name: " + actualLayoutName + " for Entity " + entityName);
            } else {
                csAssert.assertTrue(false, "Workflow Layout Show API Response for Layout Id " + layoutId + " is an Invalid JSON for Entity " + entityName);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Show of Workflow Layout Id: " + layoutId + " for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateAuditLog(int layoutId, String entityName, String expectedActionName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Layout Audit Logs.");
            ListRendererTabListData tabListDataObj = new ListRendererTabListData();

            String payload = "{\"filterMap\":{\"entityTypeId\":329,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            tabListDataObj.hitListRendererTabListData(61, 329, layoutId, payload, true);
            String auditLogTabResponse = tabListDataObj.getTabListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
                JSONArray jsonArr = new JSONObject(auditLogTabResponse).getJSONArray("data");
                String actionColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "action_name");
                String actionValue = jsonArr.getJSONObject(0).getJSONObject(actionColumnId).getString("value");

                csAssert.assertTrue(actionValue.equalsIgnoreCase(expectedActionName), "Expected Action Name: " + expectedActionName +
                        " and Actual Action Name: " + actionValue + " for Entity " + entityName);

                String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "history");
                String historyValue = jsonArr.getJSONObject(0).getJSONObject(historyColumnId).getString("value");
                Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                FieldHistory historyObj = new FieldHistory();
                String fieldHistoryResponse = historyObj.hitFieldHistory(historyId, 329, true);

                if (ParseJsonResponse.validJsonResponse(fieldHistoryResponse)) {
                    JSONObject jsonObj = new JSONObject(fieldHistoryResponse);
                    jsonArr = jsonObj.getJSONArray("value");

                    if (!jsonObj.isNull("errorMessage")) {
                        csAssert.assertTrue(false, "Error in Field History API Response: " + jsonObj.getString("errorMessage") +
                                " for Entity " + entityName);
                    } else {
                        if (expectedActionName.equalsIgnoreCase("saved")) {
                            csAssert.assertTrue(jsonArr.length() == 0, "Expected History Value length: 0 and Actual Value length: " + jsonArr.length());
                        } else {
                            csAssert.assertTrue(jsonArr.length() > 0, "History Value Array is empty.");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Field History API Response for Layout Id: " + layoutId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Audit Log of Workflow Layout Id: " + layoutId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log for Workflow Layout Id: " + layoutId + " for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateLayoutEdit(int layoutId, String editableFieldsShowPage, String editableFieldsEditPage, String editPageTabs,
                                    String showPageTabs, String entityName, int entityTypeId, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Layout Edit.");
            String newLayoutName = "API Automation Layout Edit End to End Flow";
            String editPayload = WorkflowLayoutEditPost.getPayload(layoutId, newLayoutName, editableFieldsShowPage, editableFieldsEditPage, editPageTabs,
                    showPageTabs, entityTypeId);

            String editResponse = WorkflowLayoutEditPost.getEditPostResponse(WorkflowLayoutEditPost.getApiPath(), WorkflowLayoutEditPost.getHeaders(),
                    editPayload).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(editResponse)) {
                String status = ParseJsonResponse.getStatusFromResponse(editResponse);

                if (status.equalsIgnoreCase("success")) {
                    validateLayoutListing(layoutId, newLayoutName, entityName, csAssert);

                    validateLayoutShow(layoutId, entityName, newLayoutName, csAssert);

                    validateAuditLog(layoutId, entityName, "updated", csAssert);
                } else {
                    csAssert.assertTrue(false, "Workflow Layout Edit failed for Entity " + entityName + " due to " + status);
                }
            } else {
                csAssert.assertTrue(false, "Workflow Layout Edit API Response is an Invalid JSON for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit of Workflow Layout Id: " + layoutId + " for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }
}