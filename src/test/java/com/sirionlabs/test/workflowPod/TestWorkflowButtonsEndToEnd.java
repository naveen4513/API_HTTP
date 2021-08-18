package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.workflowButtons.WorkflowButtonCreate;
import com.sirionlabs.api.workflowButtons.WorkflowButtonEdit;
import com.sirionlabs.api.workflowButtons.WorkflowButtonShow;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.WorkflowButtonsDbHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


@Listeners(value = MyTestListenerAdapter.class)
public class TestWorkflowButtonsEndToEnd {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowButtonsEndToEnd.class);
    private int newlyCreatedButtonId=0;

    @BeforeClass
    public void beforeClass() {
        new AdminHelper().loginWithClientAdminUser();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            //1002 dummy workflow_button data
            String query = "delete from workflow_button where id!=1005 and id !=1002";
            sqlObj.deleteDBEntry(query);
        } catch (Exception e) {
            logger.error("Exception while Deleting Button Data from DB having Id " + e.getMessage());
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
    public void testWorkflowButtonsFlow() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Workflow Buttons End to End flow.");

            //Create Workflow Button
            String buttonName = "API Automation Workflow End to End Flow";
            String buttonColor = "Red";
            String description = "Workflow End to End button";

            logger.info("Creating Workflow Button.");

            String payloadForCreate = WorkflowButtonCreate.getPayload(buttonName, buttonColor, description, true);

            String createResponse = WorkflowButtonCreate.getCreateResponse(WorkflowButtonCreate.getApiPath(),
                    WorkflowButtonCreate.getHeaders(), payloadForCreate).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse).getJSONObject("header").getJSONObject("response");
                newlyCreatedButtonId = jsonObj.getInt("entityId");

                //Validate Listing
                validateButtonListing(newlyCreatedButtonId, buttonName, buttonColor, csAssert);

                //Validate Button Show
                validateButtonShow(newlyCreatedButtonId, buttonName, csAssert);

                //Validate Audit Log
                validateAuditLog(newlyCreatedButtonId, "saved", csAssert);

                //Validate Button Edit
                validateButtonEdit(newlyCreatedButtonId, buttonColor, description, csAssert);

            } else {
                csAssert.assertTrue(false, "Workflow Buttons Create API Response is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Workflow Buttons End to End Flow. " + e.getMessage());
        }
        finally {
            //Delete Button
            logger.info("Deleting Workflow Button Id: {}", newlyCreatedButtonId);
            if (newlyCreatedButtonId!=0)
                  WorkflowButtonsDbHelper.deleteButtonDataInDb(newlyCreatedButtonId);
        }
        csAssert.assertAll();
    }

    private void validateButtonListing(int buttonId, String buttonName, String buttonColor, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Button Listing.");
            ListRendererListData listDataObj = new ListRendererListData();
            listDataObj.hitListRendererListData(478, true, ListDataHelper.getPayloadForListData(328, 20, 0), null);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONArray jsonArr = new JSONObject(listDataResponse).getJSONArray("data");
                int idColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                boolean buttonIdFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumn)).getString("value");

                    if (value.contains(String.valueOf(buttonId))) {
                        buttonIdFound = true;

                        //Validate Button Name in Listing
                        String nameColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "name");
                        String actualButtonName = jsonArr.getJSONObject(i).getJSONObject(nameColumnId).getString("value");
                        csAssert.assertTrue(actualButtonName.equalsIgnoreCase(buttonName), "Expected Button Name: " + buttonName + " and Actual Name: " +
                                actualButtonName + " in ListData API Response");

                        //Validate Color
                        String colorColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "color");
                        String actualColorValue = jsonArr.getJSONObject(i).getJSONObject(colorColumnId).getString("value");
                        csAssert.assertTrue(actualColorValue.equalsIgnoreCase(buttonColor), "Expected Button Color: " + buttonColor + " and Actual Color: " +
                                actualColorValue + " in ListData API Response");

                        //Validate Active
                        String activeColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "active");
                        String actualActiveValue = jsonArr.getJSONObject(i).getJSONObject(activeColumnId).getString("value");
                        csAssert.assertTrue(actualActiveValue.equalsIgnoreCase("true"), "Expected Active Value: true and Actual Value: " +
                                actualActiveValue + " in ListData API Response.");
                        break;
                    }
                }

                csAssert.assertTrue(buttonIdFound, "Workflow Button having Id: " + buttonId + " not found in Listing API Response.");
            } else {
                csAssert.assertTrue(false, "ListData API Response for Workflow Buttons is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Listing for Button Id: " + buttonId + ". " + e.getMessage());
        }
    }

    private void validateButtonShow(int buttonId, String expectedButtonName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Button Show.");
            String showResponse = WorkflowButtonShow.getShowResponse(WorkflowButtonShow.getApiPath(buttonId), WorkflowButtonShow.getHeaders()).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

                //Validate Button Name
                String actualButtonName = jsonObj.getJSONObject("name").getString("values");
                csAssert.assertTrue(actualButtonName.equalsIgnoreCase(expectedButtonName), "Expected Button Name: " + expectedButtonName +
                        " and Actual Button Name: " + actualButtonName);
            } else {
                csAssert.assertTrue(false, "Workflow Buttons Show API Response for Button Id " + buttonId + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Show of Workflow Button Id: " + buttonId + ". " + e.getMessage());
        }
    }

    private void validateAuditLog(int buttonId, String expectedActionName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Button Audit Logs.");
            ListRendererTabListData tabListDataObj = new ListRendererTabListData();

            String payload = "{\"filterMap\":{\"entityTypeId\":328,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            tabListDataObj.hitListRendererTabListData(61, 328, buttonId, payload, true);
            String auditLogTabResponse = tabListDataObj.getTabListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
                JSONArray jsonArr = new JSONObject(auditLogTabResponse).getJSONArray("data");
                String actionColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "action_name");
                String actionValue = jsonArr.getJSONObject(0).getJSONObject(actionColumnId).getString("value");

                csAssert.assertTrue(actionValue.equalsIgnoreCase(expectedActionName), "Expected Action Name: " + expectedActionName +
                        " and Actual Action Name: " + actionValue);

                String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "history");
                String historyValue = jsonArr.getJSONObject(0).getJSONObject(historyColumnId).getString("value");
                Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                FieldHistory historyObj = new FieldHistory();
                String fieldHistoryResponse = historyObj.hitFieldHistory(historyId, 328, true);

                if (ParseJsonResponse.validJsonResponse(fieldHistoryResponse)) {
                    JSONObject jsonObj = new JSONObject(fieldHistoryResponse);
                    jsonArr = jsonObj.getJSONArray("value");

                    if (!jsonObj.isNull("errorMessage")) {
                        csAssert.assertTrue(false, "Error in Field History API Response: " + jsonObj.getString("errorMessage"));
                    } else {
                        if (expectedActionName.equalsIgnoreCase("saved")) {
                            csAssert.assertTrue(jsonArr.length() == 0, "Expected History Value length: 0 and Actual Value length: " + jsonArr.length());
                        } else {
                            csAssert.assertTrue(jsonArr.length() > 0, "History Value Array is empty.");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Field History API Response for Button Id: " + buttonId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Audit Log of Workflow Button Id: " + buttonId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log for Workflow button Id: " + buttonId + ". " + e.getMessage());
        }
    }

    private void validateButtonEdit(int buttonId, String buttonColor, String buttonDescription, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Button Edit.");
            String newButtonName = "API Automation Button Edit End to End Flow";
            String editPayload = WorkflowButtonEdit.getPayload(buttonId, newButtonName, buttonColor, buttonDescription, true);
            String editResponse = WorkflowButtonEdit.getUpdateResponse(WorkflowButtonEdit.getApiPath(), WorkflowButtonEdit.getHeaders(), editPayload).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(editResponse)) {
                String status = ParseJsonResponse.getStatusFromResponse(editResponse);

                if (status.equalsIgnoreCase("success")) {
                    validateButtonShow(buttonId, newButtonName, csAssert);

                    validateAuditLog(buttonId, "updated", csAssert);
                } else {
                    csAssert.assertTrue(false, "Workflow Button Edit failed due to " + status);
                }
            } else {
                csAssert.assertTrue(false, "Workflow Buttons Edit API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit of Workflow Button Id: " + buttonId + ". " + e.getMessage());
        }
    }
}