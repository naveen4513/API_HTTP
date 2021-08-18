package com.sirionlabs.test.flowdown.action;

import com.sirionlabs.api.calendar.CalendarData;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.RoleGroupDbHelper;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate.getCreateResponse;
import static com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate.getPayloadFlowDownRoleGroup;


public class TestFlowDownForCalendar {
    private final static Logger logger = LoggerFactory.getLogger(TestFlowDownForCalendar.class);

    private String configFilePath;
    private String configFileName;
    private String testingType;
    private String expectedRoleGroupValue, actualRoleGroupValue;
    private String actualRoleGroupName;
    private int parentEntityId, childEntityId;
    private int parentRoleGroupId, childRoleGroupId;
    private String stakeholderName;
    private String stakeHolderId;
    private int month;
    private int year;
    private String calendarA;
    private String expectedEndDate;
    private int entityId;
    private int entityTypeId = 18;

    @Parameters({"TestingType"})
    @BeforeClass()
    public void beforeClass(){
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");

    }

    @Test(enabled = false)
    public void TestRoleGroupFlowDownInCalendar() {

        CustomAssert csAssert = new CustomAssert();

        // Hit the show API for selected parent and save the role group under test with its value
        parentEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "supplierid"));
        parentRoleGroupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "supplierrolegroupid"));
        month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendar", "month"));
        year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendar", "year"));
        calendarA = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendar", "calendara");
        expectedEndDate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendar", "expectedenddate");

        Show show = new Show();
        show.hitShow(1, parentEntityId);
        String response = show.getShowJsonStr();
        JSONObject obj = new JSONObject(response);
        JSONObject obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + parentRoleGroupId + "");
        String roleGroupNameParent = obj2.getString("label");
        JSONArray objArr = obj2.getJSONArray("values");
        expectedRoleGroupValue = objArr.getJSONObject(0).get("name").toString();


        // Hit the flow down API to ensure the role group has been inherited by the child entity
        String[] parentEntityTypeIdArr = {"1"};
        String[] childEntityTypeIdArr = {"18"};
        String[] roleGroupIdArr = {String.valueOf(parentRoleGroupId)};
        String[] flowEnabled = {"true"};
        String[] deletedArr = {"false"};

        String payload = getPayloadFlowDownRoleGroup(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, flowEnabled, deletedArr);
        APIResponse apiResponse = getCreateResponse(payload);
        String message = apiResponse.getResponseBody();
        if (message.contains("success")) {
            RoleGroupDbHelper db = new RoleGroupDbHelper();
            childRoleGroupId = RoleGroupDbHelper.getFlowDownRoleGroupId("Suppliers Manager","Actions");
        }

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c90594", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Hit the show API for the newly created action and verify the role group value matches that of parent
        show = new Show();
        show.hitShow(18, entityId);
        response = show.getShowJsonStr();
        obj = new JSONObject(response);
        obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + childRoleGroupId + "");
        actualRoleGroupName = obj2.getString("label");
        objArr = obj2.getJSONArray("values");
        actualRoleGroupValue = objArr.getJSONObject(0).get("name").toString();
        csAssert.assertTrue(actualRoleGroupName.equals(roleGroupNameParent), "Role Group name doesn't match from Parent to Child");
        csAssert.assertTrue(actualRoleGroupValue.equals(expectedRoleGroupValue), "Role Group values doesn't match from Parent to Child");


        // Verify the calendar Data
        String expectedStatus = "Newly Created";
        csAssert.assertTrue(verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId)), "Entity "+ entityId +" of Type "+entityTypeId+" was not present in the Calendar Data. It should be.");

        try {
            // Editing the parent contract's stakeholder
            stakeholderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "changedusername");
            stakeHolderId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "changeduserid");
            editEntityWithSpecificStakeholder("suppliers", parentEntityId, stakeholderName, stakeHolderId );

            show = new Show();
            show.hitShow(1, parentEntityId);
            response = show.getShowJsonStr();
            obj = new JSONObject(response);
            obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + parentRoleGroupId + "");
            objArr = obj2.getJSONArray("values");
            expectedRoleGroupValue = objArr.getJSONObject(0).get("name").toString();


            // Hit the show API for the action and verify the role group value matches that of parent
            show = new Show();
            show.hitShow(18, entityId);
            response = show.getShowJsonStr();
            obj = new JSONObject(response);
            obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + childRoleGroupId + "");
            actualRoleGroupName = obj2.getString("label");
            objArr = obj2.getJSONArray("values");
            actualRoleGroupValue = objArr.getJSONObject(0).get("name").toString();
            csAssert.assertTrue(actualRoleGroupValue.equals(expectedRoleGroupValue), "Role Group values doesn't match from Parent to Child");

            // Verify the calendar Data
            expectedStatus = "Newly Created";
            csAssert.assertFalse(verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId)), "Entity "+ entityId +" of Type "+entityTypeId+" was present in the Calendar Data. It should not be.");

        } catch(Exception e) {
            csAssert.fail("Something went wrong during edit parent entity check");
        }


        finally {
            // Restoring the Stakeholder in the parent entity
            stakeholderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originalusername");
            stakeHolderId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originaluserid");
            editEntityWithSpecificStakeholder("suppliers", parentEntityId, stakeholderName, stakeHolderId );
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        csAssert.assertAll();
    }

    private String createEditPayload(String editPayload){

        JSONObject editResponseJson = new JSONObject(editPayload);

        editResponseJson.remove("header");
        editResponseJson.remove("session");
        editResponseJson.remove("actions");
        editResponseJson.remove("createLinks");

        editResponseJson.getJSONObject("body").remove("layoutInfo");
        editResponseJson.getJSONObject("body").remove("globalData");
        editResponseJson.getJSONObject("body").remove("errors");

        return editResponseJson.toString();

    }

    private void editEntityWithSpecificStakeholder(String entityName, int entityId,String stakeholderName, String stakeHolderId){

        CustomAssert csAssert = new CustomAssert();
        String editPayloadOld = "";
        Edit edit = new Edit();
        String contractRoleGroupId = "rg_" + parentRoleGroupId ;

        // Edit the value of the role group under test in the parent
        String editResponse = edit.hitEdit(entityName,entityId);

        editPayloadOld = createEditPayload(editResponse);

        JSONArray valuesJsonArray = new JSONArray();
        String stakeHolderString = "{\"name\":\""+stakeholderName+"\",\"id\":"+stakeHolderId+",\"type\":2,\"email\":\"naveen@sirionqa.office\"}";

        JSONObject stakeHolderJson = new JSONObject(stakeHolderString);
        valuesJsonArray.put(0,stakeHolderJson);

        JSONObject editPayloadNew = new JSONObject(editPayloadOld);

        editPayloadNew.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(contractRoleGroupId).put("values",valuesJsonArray);

        try {
            editResponse = edit.hitEdit(entityName,editPayloadNew.toString());
        } catch (Exception e) {
            logger.error("Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
            csAssert.assertTrue(false,"Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
        }

        if(!editResponse.contains("success")){
            csAssert.assertTrue(false,"Edit Operation performed unsuccessfully for the parent entity " + entityName);
        }
    }

    public boolean verifyCalendar(CustomAssert csAssert , String expectedStatus, String entityTypeId){
        CalendarData calDataObj = new CalendarData();
        calDataObj.hitCalendarData(month, year, calendarA);
        String response = calDataObj.getCalendarDataJsonStr();
        boolean isFound = false;
        JSONArray array = new JSONArray(response);

        for (int i = 0 ; i < array.length() ; i++){
            JSONObject jsonObject= array.getJSONObject(i);
            if(Integer.parseInt(jsonObject.getString("id"))==entityId && jsonObject.getString("entityTypeId").equals(entityTypeId)) {
                long dueDate = jsonObject.getLong("start");
                String actualDueDate = DateUtils.getDateFromEpoch(Long.parseLong(dueDate + "000"), "dd-MM-yyyy");
                csAssert.assertEquals(actualDueDate,expectedEndDate, "Date mismatch for status "+expectedStatus+"");
                String actualStatus = jsonObject.getString("entityStatus");
                csAssert.assertEquals(actualStatus, expectedStatus, "Status mismatch for status "+expectedStatus+"");
                break;
            }
        }
        return isFound;
    }
}

