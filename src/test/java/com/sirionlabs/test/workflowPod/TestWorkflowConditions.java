package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.layout.ActionsOnShow;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import net.minidev.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestWorkflowConditions {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowConditions.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String workflowConditionsConfigFilePath = null;
    private static String workflowConditionsConfigFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer obligationEntityTypeId;
    private static Integer obligationEntityId= -1;
    private static Boolean deleteEntity = true;
    private ActionsOnShow action = new ActionsOnShow();
    private Edit edit = new Edit();
    private Show show = new Show();


    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        workflowConditionsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestWorkflowConditionsConfigFilePath");
        workflowConditionsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestWorkflowConditionsConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }


    @Test(description = "TC01_Workflowtasks_Visible to Stakeholders")
    public void TestC10535() throws Exception {
       String flowToTest = "flow 11";

        CustomAssert csAssert = new CustomAssert();
         try {
            logger.info("Validating Obligation Creation Flow [{}]", flowToTest);

            //Validate Obligation Creation
            logger.info("Creating Obligation for Flow [{}]", flowToTest);
            String createResponse = Obligations.createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                    true);

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success")){
                    obligationEntityId = CreateEntity.getNewEntityId(createResponse, "obligations");

                //check for the visibilty for workflow button
                    String buttonLabel = ParseConfigFile.getValueFromConfigFile(workflowConditionsConfigFilePath, workflowConditionsConfigFileName, "c10535","workflowbuttonlabel");
                    String actionRes = action.hitGetActionButton(obligationEntityTypeId,obligationEntityId);
                    JSONArray workflowButton = (JSONArray)JSONUtility.parseJson(actionRes,"$.layoutActions[*][?(@.label=='"+buttonLabel+"')].label");
                    csAssert.assertEquals(workflowButton.size(),1,
                            "workflow button is not visible even if login user persent in the stakeholder");

                //edit obligation and remove stakeholder
                    editObligation(obligationEntityTypeId,obligationEntityId,csAssert);

                    //check workflow button visibity after login user removed from stakeholder
                    String afteractionRes = action.hitGetActionButton(obligationEntityTypeId,obligationEntityId);
                    JSONArray afterworkflowButton = (JSONArray)JSONUtility.parseJson(afteractionRes,"$.layoutActions[*][?(@.label=='"+buttonLabel+"')].label");
                    csAssert.assertEquals(afterworkflowButton.size(),0,
                            "workflow button is  visible even if login user is removed from the stakeholder");



                }else {
                    csAssert.assertFalse(true,"createStatus is "+ createStatus);
                }
            }else{
                csAssert.assertTrue(false,"create response is not valid json");
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && obligationEntityId != -1) {
                logger.info("Deleting Obligation having Id {}", obligationEntityId);
                EntityOperationsHelper.deleteEntityRecord("obligations", obligationEntityId);
            }
            csAssert.assertAll();
        }
    }


    private void editObligation( int entityTypeId,int entityId,CustomAssert csAssert) throws Exception {

        String stakeholder = ParseConfigFile.getValueFromConfigFile(workflowConditionsConfigFilePath, workflowConditionsConfigFileName, "c10535","stakeholders");

        show.hitShowGetAPI(entityTypeId,entityId);
        String obligationShow = show.getShowJsonStr();
        JSONObject obligationdata = new JSONObject(obligationShow).getJSONObject("body").getJSONObject("data");
        obligationdata.put("stakeHolders", new JSONObject(stakeholder));
        obligationdata.remove("history");

        JSONObject payload = new JSONObject();
        JSONObject body = new JSONObject();
        body.put("data",obligationdata);
        payload.put("body",body);
        String editResponce = edit.hitDnoEdit("obligations",payload.toString());
        csAssert.assertEquals(JSONUtility.parseJson(editResponce,"$.header.response.status"),
                "success", "obligation "+ obligationEntityId+" description is not updated");

    }

}
