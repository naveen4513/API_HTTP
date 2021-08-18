package com.sirionlabs.test.dispute;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestDisputeTab {
    private final static Logger logger = LoggerFactory.getLogger(TestDisputeTab.class);

    private static Boolean deleteEntity = true;

    private static String configFilePath;
    private static String configFileName;
    private static String extraFieldsConfigFilePath;
    private static String extraFieldsConfigFileName;
    private static Integer obligationEntityTypeId;
    private static Integer obligationEntityId= -1;

    private TabListData tabObj = new TabListData();
    private Show show = new Show();

    private String disputeConfigFilePath;
    private String disputeConfigFileName;
    private String disputeExtraFieldsConfigFileName;
    private Integer disputeEntityTypeId;
    private Integer disputeEntityId = -1;
    private static Integer sourceReferenceTabId = 67;
    private static Integer forwardReferenceTabId = 66;

    private String issueConfigFilePath;
    private String issueConfigFileName;
    private String issueExtraFieldsConfigFileName;
    private Integer issueEntityTypeId;

    private String actionConfigFilePath;
    private String actionConfigFileName;
    private String actionExtraFieldsConfigFileName;
    private Integer actionEntityTypeId;

    private Integer disputeFromDisputeId = -1;
    private Integer actionfromDisputeId = -1;
    private Integer issuefromDisputeId = -1;


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");
        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;

        disputeConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DisputeFilePath");
        disputeConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DisputeFileName");
        disputeExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DisputeExtraFieldsFileName");
        disputeEntityTypeId = ConfigureConstantFields.getEntityIdByName("disputes");

        issueConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("IssueFilePath");
        issueConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("IssueFileName");
        issueExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("IssueExtraFieldsFileName");
        issueEntityTypeId = ConfigureConstantFields.getEntityIdByName("issues");

        actionConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ActionFilePath");
        actionConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionFileName");
        actionExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionExtraFieldsFileName");
        actionEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");

    }


    @Test(description = "C8548,C8550,C8588,C8591")
    public void TestDisputeReferenceTab() throws ParseException {
        String flowTotest="flow 11";

        CustomAssert csAssert = new CustomAssert();

        createPrerequisiteData(csAssert,flowTotest);
        validateDataFromSourceRefernceTab(csAssert,flowTotest);
        validateDataFromForwardReferenceTab(csAssert);
        deleteEntity(disputeEntityId,disputeEntityTypeId);
        //code comment as now on deletion of dispute all child action/issue/dispute get delete
       // validateForwardReferenceDeletion(csAssert);
        deleteEntity(disputeFromDisputeId,disputeEntityTypeId);
        deleteEntity(issuefromDisputeId, issueEntityTypeId);
        deleteEntity(actionfromDisputeId,actionEntityTypeId);
        deleteEntity(obligationEntityId,obligationEntityTypeId);

        csAssert.assertAll();
    }



    public void validateForwardReferenceDeletion(CustomAssert csassert){
        HashMap<Integer,Integer> forwardReference = new HashMap<>();
        forwardReference.put(disputeFromDisputeId,disputeEntityTypeId);
        forwardReference.put(actionfromDisputeId,actionEntityTypeId);
        forwardReference.put(issuefromDisputeId,issueEntityTypeId);

        for (Map.Entry<Integer, Integer>entry: forwardReference.entrySet()) {

            show.hitShowGetAPI(entry.getValue(),entry.getKey());
            String showresponse = show.getShowJsonStr();
            LinkedHashMap data = (LinkedHashMap)JSONUtility.parseJson(showresponse,"$.body.data");

            csassert.assertTrue((data.size()>0),"data is not present on show page of entityId "
            + entry.getKey()+ " and entityType "+ entry.getValue()+" after deletion of source "+ disputeEntityId
            +" and typeId"+ disputeEntityTypeId);

        }
        }




    public void validateDataFromForwardReferenceTab(CustomAssert csassert) throws ParseException {

        String citationTabResponse = tabObj.hitTabListDataV2(forwardReferenceTabId,disputeEntityTypeId,disputeEntityId);
        logger.info(citationTabResponse);
        JSONArray  data = (JSONArray) JSONUtility.parseJson(citationTabResponse,"$.data");
        for (int i = 0; i <data.size() ; i++) {

            String entityTypeId =  ((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='id')].value")).get(0).toString().split(":;")[2];
            String entityId = ((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='id')].value")).get(0).toString().split(":;")[1];

            show.hitShowGetAPI(Integer.parseInt(entityTypeId),Integer.parseInt(entityId));
            Integer sourceId = (Integer) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.sourceTitle.values.id");
            String sequenceId = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.shortCodeId.values");
            String dueDate = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.plannedCompletionDate.displayValues");
            String status = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.status.values.name");
            String title = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.name.values");

            String actaul_sequenceId = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='id')].value")).get(0).toString().split(":;")[0];
            String actaul_status = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='status')].value")).get(0);
            String actaul_name = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='name')].value")).get(0);
            String actaul_dueDate = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='due_date')].value")).get(0);
            String actaul_linkType = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='link_type')].value")).get(0);
            String actaul_linkCreationDate =(String) ((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='link_creation_date')].value")).get(0);
            String actaul_entityType = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='entity_type')].value")).get(0);
            String actaul_dependentEntity = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='dependententity')].value")).get(0);

            csassert.assertTrue(sequenceId.contains(actaul_sequenceId),"client sequenceId is not correct of "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);
            csassert.assertEquals(sourceId,disputeEntityId,"sourceId is not correct of entity "+ entityId +" and entity Type Id "+ entityTypeId);
            csassert.assertEquals(actaul_name,title,"title is not correct of entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);
            csassert.assertEquals(actaul_dueDate,dueDate,"dueDate is not correct of entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);
            csassert.assertEquals(actaul_status,status,"status is not correct of entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);
            csassert.assertEquals(actaul_linkType,"Derived","linkType is not correct of entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);
            csassert.assertEquals(actaul_linkCreationDate,DateUtils.getCurrentDateInAnyFormat("MMM-dd-yyyy","GMT"),"linkCreationDate is not correct on of entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);
            csassert.assertEquals(actaul_entityType.toLowerCase(),ConfigureConstantFields.getEntityNameById(Integer.parseInt(entityTypeId)),"entityType is not correct of entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);
            csassert.assertEquals(actaul_dependentEntity,"false","dependentEntity is not correct of entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward reference tab of dispute "+disputeEntityId);



        }

    }




public void deleteEntity(int entityId , int entityTypeId){
    if (deleteEntity && entityId != -1) {
        String entityTypeName = ConfigureConstantFields.getEntityNameById(entityTypeId);
        logger.info("Deleting "+entityTypeName+" having Id {} and entityTypeId {}", entityId, entityTypeId);
        EntityOperationsHelper.deleteEntityRecord(entityTypeName, entityId);
    }

}



    public void validateDataFromSourceRefernceTab(CustomAssert csassert, String flowToTest){

        String sourceTabResponse = tabObj.hitTabListDataV2(sourceReferenceTabId,disputeEntityTypeId,disputeEntityId);
        String sourceSequenceId = ((JSONArray)JSONUtility.parseJson(sourceTabResponse,"$.data[*].[*][?(@.columnName=='id')].value")).get(0).toString().split(":;")[0];
        String sourceName = ((JSONArray)JSONUtility.parseJson(sourceTabResponse,"$.data[*].[*][?(@.columnName=='name')].value")).get(0).toString();
        Object dueDate = ((JSONArray) JSONUtility.parseJson(sourceTabResponse, "$.data[*].[*][?(@.columnName=='due_date')].value")).get(0);
        String status = ((JSONArray)JSONUtility.parseJson(sourceTabResponse,"$.data[*].[*][?(@.columnName=='status')].value")).get(0).toString();

        show.hitShowGetAPI(obligationEntityTypeId,obligationEntityId);
        String obligationShow = show.getShowJsonStr();

        String expectedStatus = (String) JSONUtility.parseJson(obligationShow,"$.body.data.status.values.name");
        String expectedSequenceId = (String) JSONUtility.parseJson(obligationShow,"$.body.data.shortCodeId.values");
        String expectedName = (String) JSONUtility.parseJson(obligationShow,"$.body.data.name.values");

        csassert.assertTrue(expectedSequenceId.contains(sourceSequenceId),"sourceSequenceId in Source Reference tab of dispute: "+disputeEntityId+" is incorrect");
        csassert.assertEquals(sourceName,expectedName,"source title in Source Reference tab of dispute: \"+disputeEntityId+\" is incorrect\"");
        csassert.assertEquals(dueDate,null,"due date in in Source Reference tab of dispute: "+disputeEntityId+ " is incorrect ");
        csassert.assertEquals(status,expectedStatus,"status in in Source Reference tab of dispute: "+disputeEntityId+" is incorrect");

    }

    private void createPrerequisiteData(CustomAssert csAssert, String flowTotest){
        //create obligation
        obligationEntityId = createObligation(flowTotest,csAssert);
        //create dispute from obligation
        disputeEntityId = createActionIssueAndDispute(disputeConfigFilePath, disputeConfigFileName, "dispute_from_obligation", disputeExtraFieldsConfigFileName, csAssert, String.valueOf(obligationEntityId), "disputes");
        logger.info("Dispute Entity Created with entity id : " + disputeEntityId+" from obligation : "+ obligationEntityId);
         //create dispute from dispute
        disputeFromDisputeId = createActionIssueAndDispute(disputeConfigFilePath, disputeConfigFileName, "dispute_from_dispute", disputeExtraFieldsConfigFileName, csAssert, String.valueOf(disputeEntityId), "disputes");
        logger.info("Dispute Entity Created with entity id : " + disputeFromDisputeId+" from dispute : "+ disputeEntityId);
        //create action from dispute
        actionfromDisputeId = createActionIssueAndDispute(actionConfigFilePath, actionConfigFileName, "action_from_dispute", actionExtraFieldsConfigFileName, csAssert, String.valueOf(disputeEntityId), "actions");
        logger.info("Action Entity Created with entity id : " + actionfromDisputeId+" from dispute : "+ disputeEntityId);
         //create issue from dispute
        issuefromDisputeId = createActionIssueAndDispute(issueConfigFilePath, issueConfigFileName, "issue_from_dispute", issueExtraFieldsConfigFileName, csAssert, String.valueOf(disputeEntityId), "issues");
        logger.info("Issue Entity Created with entity id : " + issuefromDisputeId+" from dispute : "+ disputeEntityId);


    }


    private int createObligation(String flowToTest, CustomAssert csAssert){
        int entityId=-1;
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

                if (createStatus.equalsIgnoreCase("success")) {
                    entityId = CreateEntity.getNewEntityId(createResponse, "obligations");
                    logger.info("Created Obligation EntityId is: "+ obligationEntityId);
                }else{
                    csAssert.assertTrue(false,"create status of obligation creation is: "+createStatus);
                }
            }else{
                csAssert.assertTrue(false,"invalid json for obligation creation");
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        }
        return  entityId;
    }

    private int createActionIssueAndDispute(String configFilePath, String configFileName, String sectionName, String extraFieldsConfigFileName, CustomAssert customAssert, String sourceEntityId, String entityTypeName) {
        logger.info("*****create*****" + entityTypeName);
        try {
            ParseConfigFile.updateValueInConfigFile(configFilePath, configFileName, sectionName, "sourceid", sourceEntityId);
            String entityResponse = null;
            switch (entityTypeName) {
                case "issues":
                    entityResponse = Issue.createIssue(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, true);
                    break;
                case "disputes":
                    entityResponse = Dispute.createDispute(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, true);
                    break;
                case "actions":
                    entityResponse = Action.createAction(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, true);
                    break;
            }

            if (!ParseJsonResponse.validJsonResponse(entityResponse))
                customAssert.assertTrue(false, "create " + entityTypeName + "response Invalid");
            return (int) JSONUtility.parseJson(entityResponse, "$.header.response.entityId");
        } catch (Exception e) {
            logger.error("Exception while verifying  Create{},{}", entityTypeName, e.getMessage());
        }
        return 0;
    }

    }
