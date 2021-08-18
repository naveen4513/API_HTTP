package com.sirionlabs.test.obligation;

import com.sirionlabs.api.CreateAdhocChildCOB.CreateAdhocChildCOB;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestObligationAdhoc {

    private final static Logger logger = LoggerFactory.getLogger(TestObligationAdhoc.class);

    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer obligationEntityTypeId;
    private static Integer obligationEntityId;
    private static Boolean deleteEntity = true;
    private ListRendererTabListData tabData;
    private int childObligationTabId = 60;
    private CreateAdhocChildCOB adhoc = new CreateAdhocChildCOB();


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

        tabData = new ListRendererTabListData();

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }

   @Test(description = "C8568")
    public void TestObligationChildCreation(){
        String flowToTest = "flow 11";

     CustomAssert csAssert = new CustomAssert();
         obligationEntityId = -1;

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

                if (createStatus.equalsIgnoreCase("success"))
                    obligationEntityId = CreateEntity.getNewEntityId(createResponse, "obligations");
            }

            logger.info("Perform Entity Workflow Action For Created Obligation");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, obligationEntityId, actionLabel);
            }

            String adhocPayload = "{\"masterId\":"+obligationEntityId+",\"dueDate\":\""+ DateUtils.getCurrentDateInMM_DD_YYYY()+" 00:00:00\"}";
            String adhocResponse = adhoc.getCreateAdhocChild(adhocPayload).getResponseBody();

            if(((String)JSONUtility.parseJson(adhocResponse,"$[0].errorMessage")).contains("created successfully.")){
                if(validateCOBCreation()!=null){
                    deleteCOBs(validateCOBCreation());
                }
                else {
                    csAssert.assertFalse(true,"COB are not created by schedular");
                }

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




    private JSONArray validateCOBCreation(){
        JSONArray cobIds = null;
        String payload = "{\"filterMap\":{\"entityTypeId\":13,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        tabData.hitListRendererTabListData(childObligationTabId,obligationEntityTypeId,obligationEntityId,payload);
        String  COBDataResponse = tabData.getTabListDataJsonStr();
        JSONArray data =  (JSONArray) JSONUtility.parseJson(COBDataResponse,"$.data");
        if(data.size()>0){
            logger.info( data.size() +"Child Obligations created by schedular");
            cobIds = (JSONArray) JSONUtility.parseJson(data.toString(), "[*].[*][?(@.columnName =='id')].value");
            logger.info("COB created are "+ cobIds.toString() );
        }
        else {
            logger.info("COB's are not created yet");
        }

        return cobIds;

    }

    private void deleteCOBs(JSONArray cobIdArray){
        for (int i = 0; i <cobIdArray.size() ; i++) {
            String  COBID    = cobIdArray.get(i).toString().split(":;")[1];
            logger.info("Deleting Obligation having Id {}", COBID);
            EntityOperationsHelper.deleteEntityRecord("child obligations", Integer.parseInt(COBID));
        }
    }


}
