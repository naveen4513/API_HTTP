package com.sirionlabs.test.todo;

import com.sirionlabs.api.AllCommonAPI.APIExecutorCommon;
import com.sirionlabs.api.AllCommonAPI.AllCommonAPI;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ToDoAdmin {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ToDoAdmin.class);
    private JSONObject modifiedJsonObject;
    private String savedWFStatus;
    private final int gbEntityTypeID = 86;
    private List<Integer> allGBId = new ArrayList<>();
    private List<String> task = new ArrayList<>();

    @DataProvider
    public Object[][] gbDataProvider() {
        List<Object[]> allTestData = new ArrayList<>();
        String gbStatus = "Send For Internal Review";
        boolean currentDate = false;
        allTestData.add(new Object[]{gbStatus, currentDate});
        gbStatus = "Send For Internal Review,Internal Review Complete";
        currentDate = false;
        allTestData.add(new Object[]{gbStatus, currentDate});
        gbStatus = "Send For Internal Review,Internal Review Complete,Send For Client Review";
        currentDate = false;
        allTestData.add(new Object[]{gbStatus, currentDate});
        gbStatus = "Send For Internal Review";
        currentDate = true;
        allTestData.add(new Object[]{gbStatus, currentDate});
        gbStatus = "Send For Internal Review,Internal Review Complete";
        currentDate = true;
        allTestData.add(new Object[]{gbStatus, currentDate});
        gbStatus = "Send For Internal Review,Internal Review Complete,Send For Client Review";
        currentDate = true;
        allTestData.add(new Object[]{gbStatus, currentDate});

        return allTestData.toArray(new Object[0][]);

    }

    @Test(dataProvider = "gbDataProvider", priority = 0)
    public void createGB(String gbStatus, boolean currentDate) {
        CustomAssert csAssert = new CustomAssert();
        int gbEntityId = 0;
        try {
            logger.info("************Create Gb****************");
            String governanceBodiesResponse;
            if (currentDate)
                governanceBodiesResponse = GovernanceBody.createGB("governance_bodies_aid", true);

            else
                governanceBodiesResponse = GovernanceBody.createGB("governance_bodies_todo", true);

            if (ParseJsonResponse.validJsonResponse(governanceBodiesResponse)) {
                gbEntityId = CreateEntity.getNewEntityId(governanceBodiesResponse);
                allGBId.add(gbEntityId);
            }
            if (gbEntityId == -1) {
                csAssert.assertTrue(false, "GB IS not Creating");
                throw new SkipException("GB is not creating");
            }
            logger.info("Governance Body Created with Entity id: " + gbEntityId);
            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = gbStatus.split(",");
            if (!workFlowStep[0].equalsIgnoreCase("Delete Case")) {
                for (String actionLabel : workFlowStep) {
                    logger.info(actionLabel);
                    entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityId, actionLabel.trim());
                }
            }
        } catch (Exception e) {
            logger.error("Exception while creating GB");
            csAssert.assertTrue(false, e.getMessage());
        }

        csAssert.assertAll();
    }

    @Test
    public void testToDoExcludeFilterAndApprovals() {

        CustomAssert customAssert = new CustomAssert();
        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;
        AllCommonAPI allCommonAPI = new AllCommonAPI();
        APIResponse apiResponse;
        APIExecutorCommon apiExecutorCommon = new APIExecutorCommon();
        HashMap<String, List<String>> allStatus = new HashMap<>();
        try {
            new AdminHelper().loginWithClientAdminUser();
            apiResponse = apiExecutorCommon.setApiPath(allCommonAPI.getTodoClusteringWorkFlowStatus())
                    .setHeaders("Accept", "application/json, text/plain, */*")
                    .setHeaders("Content-Type", "application/json;charset=UTF-8")
                    .setMethodType("get")
                    .getResponse();

            String workflowStatus = apiResponse.getResponseBody();
            JSONArray jsonArrayStatus = new JSONObject(workflowStatus).getJSONObject("Governance Body").getJSONArray("status");

            for (int i = 0; i < jsonArrayStatus.length(); i++) {
                task.add(jsonArrayStatus.getJSONObject(i).getString("name"));
            }

            if (ParseJsonResponse.validJsonResponse(workflowStatus)) {

                apiResponse = apiExecutorCommon.setApiPath(allCommonAPI.getTodoClusteringSavedWFStatus())
                        .setHeaders("Accept", "application/json, text/plain, */*")
                        .setHeaders("Content-Type", "application/json;charset=UTF-8")
                        .setMethodType("get")
                        .getResponse();


                savedWFStatus = apiResponse.getResponseBody();
                if (ParseJsonResponse.validJsonResponse(savedWFStatus)) {

                    JSONArray jsonArray = new JSONObject(savedWFStatus).getJSONArray("entityType");
                    for (int i = 0; i < jsonArray.length(); i++) {

                        if (new JSONObject(savedWFStatus).getJSONArray("entityType").getJSONObject(i).getInt("entityTypeId") == gbEntityTypeID) {

                            JSONArray excludeFromToDo = new JSONObject(savedWFStatus).getJSONArray("entityType").getJSONObject(i).getJSONArray("excludeFromToDo");
                            JSONArray approvals = new JSONObject(savedWFStatus).getJSONArray("entityType").getJSONObject(i).getJSONArray("approvals");

                            List<String> excludeFromToDoList = new ArrayList<>();
                            List<String> approvalsList = new ArrayList<>();
                            for (int j = 0; j < excludeFromToDo.length(); j++) {
                                excludeFromToDoList.add(excludeFromToDo.getJSONObject(j).getString("name"));
                            }

                            for (int j = 0; j < approvals.length(); j++) {
                                approvalsList.add(approvals.getJSONObject(j).getString("name"));
                            }

                            task.removeAll(excludeFromToDoList);
                            task.removeAll(approvalsList);

                            allStatus.put("excludeFromToDo", excludeFromToDoList);
                            allStatus.put("approvals", approvalsList);

                            modifiedJsonObject = new JSONObject(savedWFStatus).put("entityType", new JSONObject(savedWFStatus).getJSONArray("entityType").put(i, new JSONObject(savedWFStatus).getJSONArray("entityType").getJSONObject(i).put("excludeFromToDo", new JSONObject(workflowStatus).getJSONObject("Governance Body").getJSONArray("status"))));
                        }
                    }

                    apiResponse = apiExecutorCommon.setApiPath(allCommonAPI.getTodoClusteringSaveStatus())
                            .setHeaders("Accept", "application/json, text/plain, */*")
                            .setHeaders("Content-Type", "application/json;charset=UTF-8")
                            .setPayload(modifiedJsonObject.toString())
                            .setMethodType("post")
                            .getResponse();


                    String saveStatus = apiResponse.getResponseBody();
                    if (ParseJsonResponse.validJsonResponse(saveStatus)) {


                        JSONObject jsonObjectSaveStatus = new JSONObject(saveStatus);

                        String errorMessages = jsonObjectSaveStatus.get("errorMessages").toString();
                        String message = jsonObjectSaveStatus.getString("entity");
                        boolean success = jsonObjectSaveStatus.getBoolean("success");

                        if (errorMessages.equalsIgnoreCase("null") && message.equalsIgnoreCase("Statuses Successfully Saved") & success) {

                            new Check().hitCheck(lastUserName, lastUserPassword);
                            apiResponse = apiExecutorCommon.setApiPath(allCommonAPI.getPendingActionsDaily(gbEntityTypeID))
                                    .setHeaders("Accept", "application/json, text/plain, */*")
                                    .setHeaders("Content-Type", "application/json;charset=UTF-8")
                                    .setMethodType("get")
                                    .getResponse();

                            String pending_actions = apiResponse.getResponseBody();

                            if (ParseJsonResponse.validJsonResponse(pending_actions)) {

                                if (!new JSONArray(pending_actions).toList().isEmpty()) {
                                    customAssert.assertTrue(false, "All Daily Pending Action Should be Null because all status add in excludeFromToDo");
                                }

                            }
                        } else {
                            customAssert.assertTrue(false, "Statuses not Saved Successfully in excludeFromToDo" + errorMessages);
                        }
                    }
                }

            }

            testToDoExcludeNegativeFilter(savedWFStatus, allCommonAPI, apiExecutorCommon, allStatus, task, customAssert);
            testToDoExcludeNegativeFilterUpcoming(allCommonAPI, apiExecutorCommon, allStatus, task, customAssert);

        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception While Verifying Todo Status on client admin");
        } finally {
            logger.info("******************GB Deleted***********************************");
            for (int gbId : allGBId) {
                ShowHelper.deleteEntity("governance body", gbEntityTypeID, gbId);
            }
        }
        customAssert.assertAll();
    }

    private void testToDoExcludeNegativeFilterUpcoming(AllCommonAPI allCommonAPI, APIExecutorCommon apiExecutorCommon, HashMap<String, List<String>> allStatus, List<String> task, CustomAssert customAssert) {

        try {
            APIResponse apiResponse = apiExecutorCommon.setApiPath(allCommonAPI.getPendingActionsWeekly(gbEntityTypeID))
                    .setHeaders("Accept", "application/json, text/plain, */*")
                    .setHeaders("Content-Type", "application/json;charset=UTF-8")
                    .setMethodType("get")
                    .getResponse();
            String pending_actions = apiResponse.getResponseBody();
            if (ParseJsonResponse.validJsonResponse(pending_actions)) {
                if (new JSONArray(pending_actions).toList().isEmpty()) {
                    customAssert.assertTrue(false, "");
                } else {
                    JSONArray jsonArray = new JSONArray(pending_actions);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        if (jsonArray.getJSONObject(i).getBoolean("approval")) {
                            customAssert.assertTrue(false, "approval should not be present in upcoming meeting");
                        } else {
                            if (!task.contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                customAssert.assertTrue(false, "status not found in task" + jsonArray.getJSONObject(i).getString("statusName"));
                            } else if (allStatus.get("approvals").contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                customAssert.assertTrue(false, "status not found in approvals");
                            } else if (allStatus.get("excludeFromToDo").contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                customAssert.assertTrue(false, "exclude from to do status should not be found in upcoming task" + jsonArray.getJSONObject(i).getString("statusName"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception while verifying upcoming todo status");
        }
        customAssert.assertAll();
    }

    private void testToDoExcludeNegativeFilter(String savedWFStatus, AllCommonAPI allCommonAPI, APIExecutorCommon apiExecutorCommon, HashMap<String, List<String>> allStatus, List<String> task, CustomAssert customAssert) {
        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        try {
            new AdminHelper().loginWithClientAdminUser();
            APIResponse apiResponse = apiExecutorCommon.setApiPath(allCommonAPI.getTodoClusteringSaveStatus())
                    .setHeaders("Accept", "application/json, text/plain, */*")
                    .setHeaders("Content-Type", "application/json;charset=UTF-8")
                    .setPayload(savedWFStatus)
                    .setMethodType("post")
                    .getResponse();
            String saveStatus = apiResponse.getResponseBody();
            if (ParseJsonResponse.validJsonResponse(saveStatus)) {
                JSONObject jsonObjectSaveStatus = new JSONObject(saveStatus);
                String errorMessages = jsonObjectSaveStatus.get("errorMessages").toString();
                String message = jsonObjectSaveStatus.getString("entity");
                boolean success = jsonObjectSaveStatus.getBoolean("success");
                if (errorMessages.equalsIgnoreCase("null") && message.equalsIgnoreCase("Statuses Successfully Saved") & success) {
                    new Check().hitCheck(lastUserName, lastUserPassword);
                    apiResponse = apiExecutorCommon.setApiPath("/pending-actions/daily/86/?")
                            .setHeaders("Accept", "application/json, text/plain, */*")
                            .setHeaders("Content-Type", "application/json;charset=UTF-8")
                            .setMethodType("get")
                            .getResponse();
                    String pending_actions = apiResponse.getResponseBody();
                    if (ParseJsonResponse.validJsonResponse(pending_actions)) {
                        if (new JSONArray(pending_actions).toList().isEmpty()) {
                            customAssert.assertTrue(false, "pending actions should not be null");
                        } else {
                            JSONArray jsonArray = new JSONArray(pending_actions);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                if (jsonArray.getJSONObject(i).getBoolean("approval")) {
                                    if (!allStatus.get("approvals").contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                        customAssert.assertTrue(false, "status not found in approvals" + jsonArray.getJSONObject(i).getString("statusName"));
                                    } else if (allStatus.get("excludeFromToDo").contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                        customAssert.assertTrue(false, "approvals status found in excludeFromToDo" + jsonArray.getJSONObject(i).getString("statusName"));
                                    } else if (task.contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                        customAssert.assertTrue(false, "approvals status found in task" + jsonArray.getJSONObject(i).getString("statusName"));
                                    }
                                } else {
                                    if (!task.contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                        customAssert.assertTrue(false, "status not found in task" + jsonArray.getJSONObject(i).getString("statusName"));
                                    } else if (allStatus.get("approvals").contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                        customAssert.assertTrue(false, "task status found in approvals");
                                    } else if (allStatus.get("excludeFromToDo").contains(jsonArray.getJSONObject(i).getString("statusName"))) {
                                        customAssert.assertTrue(false, "task status found in excludeFromToDo");
                                    }
                                }
                            }

                        }

                    }
                } else {
                    customAssert.assertTrue(false, "status not saved successfully" + errorMessages);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception while verify today status in todo");
        }
        customAssert.assertAll();
    }
}
