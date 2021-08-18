package com.sirionlabs.test.flowdown.issue;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import static com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.RoleGroupDbHelper;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Issue;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


public class TestFlowDownForIssue {
    private final static Logger logger = LoggerFactory.getLogger(TestFlowDownForIssue.class);

    private String configFilePath;
    private String configFileName;
    private String testingType;
    private String expectedRoleGroupValue, actualRoleGroupValue;
    private String actualRoleGroupName;
    private int parentEntityId, childEntityId;
    private int parentRoleGroupId, childRoleGroupId;
    private String stakeholderName;
    private String stakeHolderId;

    @Parameters({"TestingType"})
    @BeforeClass(groups = { "minor" })
    public void beforeClass(){
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");

    }

    @Test(enabled = true)
    public void TestRoleGroupFlowDownForContract() {

        CustomAssert csAssert = new CustomAssert();

        // Hit the show API for selected parent contract and save the role group under test with its value
        parentEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "contractid"));
        parentRoleGroupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "contractrolegroupid"));

        Show show = new Show();
        show.hitShow(61, parentEntityId);
        String response = show.getShowJsonStr();
        JSONObject obj = new JSONObject(response);
        JSONObject obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + parentRoleGroupId + "");
        String roleGroupNameParent = obj2.getString("label");
        JSONArray objArr = obj2.getJSONArray("values");
        expectedRoleGroupValue = objArr.getJSONObject(0).get("name").toString();


        // Hit the flow down API to ensure the role group has been inherited by the child entity
        String[] parentEntityTypeIdArr = {"61"};
        String[] childEntityTypeIdArr = {"17"};
        String[] roleGroupIdArr = {String.valueOf(parentRoleGroupId)};
        String[] flowEnabled = {"true"};
        String[] deletedArr = {"false"};

        String payload = getPayloadFlowDownRoleGroup(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, flowEnabled, deletedArr);
        APIResponse apiResponse = getCreateResponse(payload);
        String message = apiResponse.getResponseBody();
        if (message.contains("success")) {
            RoleGroupDbHelper db = new RoleGroupDbHelper();
            childRoleGroupId = RoleGroupDbHelper.getFlowDownRoleGroupId("Contract Manager","Issues");
        }

        // Create a new Issue from the selected parent contract
        String issueResponseString = Issue.createIssue("src/test/resources/Helper/EntityCreation/Issue", "issue.cfg", "src/test/resources/Helper/EntityCreation/Issue", "issueExtraFields.cfg", "flowdown contract", false);
        childEntityId = CreateEntity.getNewEntityId(issueResponseString, "issue");

        // Hit the show API for the newly created Issue and verify the role group value matches that of parent
        show = new Show();
        show.hitShow(17, childEntityId);
        response = show.getShowJsonStr();
        obj = new JSONObject(response);
        obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + childRoleGroupId + "");
        actualRoleGroupName = obj2.getString("label");
        objArr = obj2.getJSONArray("values");
        actualRoleGroupValue = objArr.getJSONObject(0).get("name").toString();
        csAssert.assertTrue(actualRoleGroupName.equals(roleGroupNameParent), "Role Group name doesn't match from Parent to Child");
        csAssert.assertTrue(actualRoleGroupValue.equals(expectedRoleGroupValue), "Role Group values doesn't match from Parent to Child");


        try {
            // Editing the parent entity's stakeholder
            stakeholderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "changedusername");
            stakeHolderId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "changeduserid");
            editEntityWithSpecificStakeholder("contracts", parentEntityId, stakeholderName, stakeHolderId );

            show = new Show();
            show.hitShow(61, parentEntityId);
            response = show.getShowJsonStr();
            obj = new JSONObject(response);
            obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + parentRoleGroupId + "");
            objArr = obj2.getJSONArray("values");
            expectedRoleGroupValue = objArr.getJSONObject(0).get("name").toString();


            // Hit the show API for the Issue and verify the role group value matches that of parent
            show = new Show();
            show.hitShow(17, childEntityId);
            response = show.getShowJsonStr();
            obj = new JSONObject(response);
            obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + childRoleGroupId + "");
            actualRoleGroupName = obj2.getString("label");
            objArr = obj2.getJSONArray("values");
            actualRoleGroupValue = objArr.getJSONObject(0).get("name").toString();
            csAssert.assertTrue(actualRoleGroupValue.equals(expectedRoleGroupValue), "Role Group values doesn't match from Parent to Child");


        } catch(Exception e) {
            csAssert.fail("Sommething went wrong during edit parent entity check");
        }


        finally {
            // Restoring the Stakeholder in the parent entity
            stakeholderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originalusername");
            stakeHolderId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originaluserid");
            editEntityWithSpecificStakeholder("contracts", parentEntityId, stakeholderName, stakeHolderId );
            EntityOperationsHelper.deleteEntityRecord("actions", childEntityId);
        }
        EntityOperationsHelper.deleteEntityRecord("issues", childEntityId);
        csAssert.assertAll();
    }

    @Test(groups = { "minor" }, enabled = true)
    public void TestRoleGroupFlowDownForSupplier() {

        CustomAssert csAssert = new CustomAssert();

        // Hit the show API for selected parent contract and save the role group under test with its value
        parentEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "supplierid"));
        parentRoleGroupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "supplierrolegroupid"));

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
        String[] childEntityTypeIdArr = {"17"};
        String[] roleGroupIdArr = {String.valueOf(parentRoleGroupId)};
        String[] flowEnabled = {"true"};
        String[] deletedArr = {"false"};

        String payload = getPayloadFlowDownRoleGroup(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, flowEnabled, deletedArr);
        APIResponse apiResponse = getCreateResponse(payload);
        String message = apiResponse.getResponseBody();
        if (message.contains("success")) {
            RoleGroupDbHelper db = new RoleGroupDbHelper();
            childRoleGroupId = RoleGroupDbHelper.getFlowDownRoleGroupId("Suppliers Manager","Issues");
        }

        // Create a new Issue from the selected parent contract
        String issueResponseString = Issue.createIssue("src/test/resources/Helper/EntityCreation/Issue", "issue.cfg", "src/test/resources/Helper/EntityCreation/Issue", "issueExtraFields.cfg", "flowdown supplier", true);
        childEntityId = CreateEntity.getNewEntityId(issueResponseString, "issue");

        // Hit the show API for the newly created Issue and verify the role group value matches that of parent
        show = new Show();
        show.hitShow(17, childEntityId);
        response = show.getShowJsonStr();
        obj = new JSONObject(response);
        obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + childRoleGroupId + "");
        actualRoleGroupName = obj2.getString("label");
        objArr = obj2.getJSONArray("values");
        actualRoleGroupValue = objArr.getJSONObject(0).get("name").toString();
        csAssert.assertTrue(actualRoleGroupName.equals(roleGroupNameParent), "Role Group name doesn't match from Parent to Child");
        csAssert.assertTrue(actualRoleGroupValue.equals(expectedRoleGroupValue), "Role Group values doesn't match from Parent to Child");


        try {
            // Editing the parent entity's stakeholder
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


            // Hit the show API for the issue and verify the role group value matches that of parent
            show = new Show();
            show.hitShow(17, childEntityId);
            response = show.getShowJsonStr();
            obj = new JSONObject(response);
            obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + childRoleGroupId + "");
            actualRoleGroupName = obj2.getString("label");
            objArr = obj2.getJSONArray("values");
            actualRoleGroupValue = objArr.getJSONObject(0).get("name").toString();
            csAssert.assertTrue(actualRoleGroupValue.equals(expectedRoleGroupValue), "Role Group values doesn't match from Parent to Child");


        } catch(Exception e) {
            csAssert.fail("Sommething went wrong during edit parent entity check");
        }


        finally {
            // Restoring the Stakeholder in the parent entity
            stakeholderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originalusername");
            stakeHolderId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originaluserid");
            editEntityWithSpecificStakeholder("suppliers", parentEntityId, stakeholderName, stakeHolderId );
            EntityOperationsHelper.deleteEntityRecord("actions", childEntityId);
        }
        EntityOperationsHelper.deleteEntityRecord("issues", childEntityId);
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
        String roleGroupId = "rg_" + parentRoleGroupId ;

        // Edit the value of the role group under test in the parent
        String editResponse = edit.hitEdit(entityName,entityId);

        editPayloadOld = createEditPayload(editResponse);

        JSONArray valuesJsonArray = new JSONArray();
        String stakeHolderString = "{\"name\":\""+stakeholderName+"\",\"id\":"+stakeHolderId+",\"type\":2,\"email\":\"naveen@sirionqa.office\"}";

        JSONObject stakeHolderJson = new JSONObject(stakeHolderString);
        valuesJsonArray.put(0,stakeHolderJson);

        JSONObject editPayloadNew = new JSONObject(editPayloadOld);

        editPayloadNew.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(roleGroupId).put("values",valuesJsonArray);

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
}

