package com.sirionlabs.test.obligation;

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

public class TestObligationLinkageTab {
    private final static Logger logger = LoggerFactory.getLogger(TestObligationLinkageTab.class);

    private static String configFilePath;
    private static String configFileName;
    private static String extraFieldsConfigFilePath;
    private static String extraFieldsConfigFileName;
    private static Integer obligationEntityTypeId;
    private static Integer contractEntityTypeId;
    private static Integer obligationEntityId;
    private static Boolean deleteEntity = true;
    private TabListData tabObj = new TabListData();
    private static Integer linkageTabSourceListId = 67;
    private static Integer linkageTabForwardCitationListId = 66;
    private Show show;

    private String disputeConfigFilePath;
    private String disputeConfigFileName;
    private String disputeExtraFieldsConfigFileName;
    private int disputeEntityTypeId;
    private String issueConfigFilePath;
    private String issueConfigFileName;
    private String issueExtraFieldsConfigFileName;
    private int issueEntityTypeId;
    private String actionConfigFilePath;
    private String actionConfigFileName;
    private String actionExtraFieldsConfigFileName;
    private int actionEntityTypeId;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");
        contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
        show = new Show();
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


    @Test(description = "C9060,C9036")
    public void TestObligationLinkageTab() {
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

                if (createStatus.equalsIgnoreCase("success")) {
                    obligationEntityId = CreateEntity.getNewEntityId(createResponse, "obligations");
                }

                validateDataFromObligationSourceTab(csAssert,flowToTest);

                int disputeId = createActionIssueAndDispute(disputeConfigFilePath, disputeConfigFileName, "dispute_from_obligation", disputeExtraFieldsConfigFileName, csAssert, String.valueOf(obligationEntityId), "disputes");
                logger.info("Dispute Entity Created with entity id : " + disputeId);
                int issueId = createActionIssueAndDispute(issueConfigFilePath, issueConfigFileName, "issue_from_obligation", issueExtraFieldsConfigFileName, csAssert, String.valueOf(obligationEntityId), "issues");
                logger.info("Issue Entity Created with entity id : " + issueId);
                int actionId = createActionIssueAndDispute(actionConfigFilePath, actionConfigFileName, "action_from_obligation", actionExtraFieldsConfigFileName, csAssert, String.valueOf(obligationEntityId), "actions");
                logger.info("Action Entity Created with entity id : " + actionId);
                validateDataFromObligationeForwardCitationTab(csAssert);

            }


        } catch (Exception e) {
                    csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
                } finally {
                    if (deleteEntity && obligationEntityId != -1) {
                        logger.info("Deleting Obligation having Id {}", obligationEntityId);
                       EntityOperationsHelper.deleteEntityRecord("obligations", obligationEntityId);
                    }
                    csAssert.assertAll();
                }


    }


    public void validateDataFromObligationeForwardCitationTab(CustomAssert csassert){

        String citationTabResponse = tabObj.hitTabListDataV2(linkageTabForwardCitationListId,obligationEntityTypeId,obligationEntityId);
        logger.info(citationTabResponse);
        JSONArray  data = (JSONArray) JSONUtility.parseJson(citationTabResponse,"$.data");
        for (int i = 0; i <data.size() ; i++) {

           String entityTypeId =  ((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='id')].value")).get(0).toString().split(":;")[2];
            String entityId = ((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='id')].value")).get(0).toString().split(":;")[1];

            show.hitShowGetAPI(Integer.parseInt(entityTypeId),Integer.parseInt(entityId));
            String entityShow = show.getShowJsonStr();
            Integer sourceId = (Integer) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.sourceTitle.values.id");
            String dueDate = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.plannedCompletionDate.displayValues");
            String status = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.status.values.name");
            String title = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.name.values");

            String actaul_status = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='status')].value")).get(0);
            String actaul_name = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='name')].value")).get(0);
            String actaul_dueDate = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='due_date')].value")).get(0);
            String actaul_linkType = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='link_type')].value")).get(0);
            String actaul_linkCreationDate =(String) ((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='link_creation_date')].value")).get(0);
            String actaul_entityType = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='entity_type')].value")).get(0);
            String actaul_dependentEntity = (String)((JSONArray)JSONUtility.parseJson(data.toString(),"$["+i+"].[*][?(@.columnName=='dependententity')].value")).get(0);



            csassert.assertEquals(sourceId,obligationEntityId,"sourceId is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId);
            csassert.assertEquals(actaul_name,title,"title is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward citation tab of obligation "+obligationEntityId);
            csassert.assertEquals(actaul_dueDate,dueDate,"dueDate is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward citation tab of obligation "+obligationEntityId);
            csassert.assertEquals(actaul_status,status,"status is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward citation tab of obligation "+obligationEntityId);
            csassert.assertEquals(actaul_linkType,"Derived","linkType is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward citation tab of obligation "+obligationEntityId);
            csassert.assertEquals(actaul_linkCreationDate,DateUtils.getCurrentDateInAnyFormat("MMM-dd-yyyy","GMT"),"linkCreationDate is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward citation tab of obligation "+obligationEntityId);
            csassert.assertEquals(actaul_entityType.toLowerCase(),ConfigureConstantFields.getEntityNameById(Integer.parseInt(entityTypeId)),"entityType is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward citation tab of obligation "+obligationEntityId);
            csassert.assertEquals(actaul_dependentEntity,"false","dependentEntity is not correct on show page if entity "+ entityId +" and entity Type Id "+ entityTypeId+ "in forward citation tab of obligation "+obligationEntityId);



        }

    }



    public void validateDataFromObligationSourceTab(CustomAssert csassert, String flowToTest){

        String sourceTabResponse = tabObj.hitTabListDataV2(linkageTabSourceListId,obligationEntityTypeId,obligationEntityId);
       String sourceDBId = JSONUtility.parseJson(sourceTabResponse,"$.data[*].[*][?(@.columnName=='id')].value").toString().split(":;")[1];
        String sourceName = ((JSONArray)JSONUtility.parseJson(sourceTabResponse,"$.data[*].[*][?(@.columnName=='name')].value")).get(0).toString();
        Object dueDate = ((JSONArray) JSONUtility.parseJson(sourceTabResponse, "$.data[*].[*][?(@.columnName=='due_date')].value")).get(0);
        String status = ((JSONArray)JSONUtility.parseJson(sourceTabResponse,"$.data[*].[*][?(@.columnName=='status')].value")).get(0).toString();

        show.hitShowGetAPI(contractEntityTypeId,Integer.parseInt(sourceDBId));
        String contractShow = show.getShowJsonStr();

        String expectedStatus = (String) JSONUtility.parseJson(contractShow,"$.body.data.status.values.name");;
         String expectedSourceDBId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,flowToTest,"sourceid");
         String expectedSourceName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,flowToTest,"sourcename");;

     csassert.assertEquals(sourceDBId,expectedSourceDBId,"source Id in linkage/source tab is incorrect");
     csassert.assertEquals(sourceName,expectedSourceName,"source Name in linkage/source tab is incorrect");
     csassert.assertEquals(dueDate,null,"due date in linkage/source tab is incorrect");
     csassert.assertEquals(status,expectedStatus,"status in linkage/source tab is incorrect");

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
