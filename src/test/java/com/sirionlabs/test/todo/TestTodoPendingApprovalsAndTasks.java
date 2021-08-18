package com.sirionlabs.test.todo;

import com.sirionlabs.api.clientAdmin.todoParams.TodoParamsCreateForm;
import com.sirionlabs.api.todo.TodoPendingApproval;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestTodoPendingApprovalsAndTasks {

    private final static Logger logger = LoggerFactory.getLogger(TestTodoPendingApprovalsAndTasks.class);

    private List<String> entitiesToTest;


    @BeforeClass
    public void beforeClass() {
        String[] entitiesArr = {"suppliers", "actions"};
        entitiesToTest = new ArrayList<>(Arrays.asList(entitiesArr));
    }

    @Test
    public void testTodoPendingApprovals() {
        CustomAssert csAssert = new CustomAssert();

        for (String entityName : entitiesToTest) {
            logger.info("Validating Todo Pending Approvals for Entity: {}", entityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            String apiPath = TodoPendingApproval.getApiPath(entityTypeId, "daily", 0, 1000);
            APIResponse response = TodoPendingApproval.getPendingApprovalResponse(apiPath, TodoPendingApproval.getHeaders());

            validateTodoResponseData(entityName, entityTypeId, response, csAssert);
        }

        csAssert.assertAll();
    }

    private void validateTodoResponseData(String entityName, int entityTypeId, APIResponse response, CustomAssert csAssert) {
        try {
            csAssert.assertTrue(response.getResponseCode() == 200, "Expected Response Code: 200 and Actual Response Code: " +
                    response.getResponseCode() + " for Pending Approval Daily and Entity " + entityName);

            String responseBody = response.getResponseBody();

            if (ParseJsonResponse.validJsonResponse(responseBody)) {
                String createFormResponse = TodoParamsCreateForm.getCreateFormResponse(TodoParamsCreateForm.getAPIPath(1), TodoParamsCreateForm.getHeaders());

                JSONArray jsonArr = new JSONArray(responseBody);

                for (int i = 0; i < jsonArr.length(); i++) {
                    String actualStatusName = jsonArr.getJSONObject(i).getString("statusName");
                    int recordId = jsonArr.getJSONObject(i).getInt("id");
                    String showResponse = ShowHelper.getShowResponseVersion2(entityTypeId, recordId);

                    List<String> allSelectedStatus = TodoParamsCreateForm.getAllSelectedStatusForEntity(createFormResponse, entityTypeId);

                    //Validate Status
                    validateStatusField(entityName, recordId, createFormResponse, allSelectedStatus, actualStatusName, csAssert);

                    //Validate Stakeholder

                }


            } else {
                csAssert.assertTrue(false, "Todo Pending Approval API Response for Entity: " + entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Todo Pending Approval for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private void validateStatusField(String entityName, int recordId, String createFormResponse, List<String> allSelectedStatus, String actualStatusName,
                                     CustomAssert csAssert) {
        try {
            if (allSelectedStatus == null) {
                csAssert.assertTrue(false, "Couldn't get All Selected Status for Entity " + entityName);
                return;
            }

            if (!allSelectedStatus.contains(actualStatusName)) {
                //Validate with Parent Entity

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Status field for Entity: " + entityName + " and Record Id: " +
                    recordId + ". " + e.getMessage());
        }
    }

    private void validateStakeholderField(String entityName, int recordId, String createFormResponse, CustomAssert csAssert) {
        try {

        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Stakeholder Field for Entity: " + entityName + " and Record Id: " +
                    recordId + ". " + e.getMessage());
        }
    }
}