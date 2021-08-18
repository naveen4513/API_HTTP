package com.sirionlabs.test.obligation;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.reportRenderer.ReportRenderListReportJson;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class TestObligationMisc {

    private final static Logger logger = LoggerFactory.getLogger(TestObligationMisc.class);
    private static String configFilePath;
    private static String configFileName;
    private static String extraFieldsConfigFilePath;
    private static String extraFieldsConfigFileName;
    private static Integer obligationEntityTypeId;
    private static Integer cobEntityTypeId;
    private static Integer obligationEntityId;
    private static Boolean deleteEntity = true;
    private ReportRenderListReportJson reportRenderListReportJsonObj = new ReportRenderListReportJson();
    private AdminHelper adminHelper = new AdminHelper();
    private Show show = new Show();
    private Edit edit = new Edit();
    private ListRendererTabListData tabData = new ListRendererTabListData();
    private int childObligationTabId = 60;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");
        cobEntityTypeId = ConfigureConstantFields.getEntityIdByName("child obligations");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;

    }



    @Test(description = "C8878,C8879")
    public  void TestReportAccess(){
        String reportName= "Child Obligations - Aging";
        CustomAssert csAssert = new CustomAssert();
        JSONArray agingReportObj = null;

        //Hit User report renderer Api and get report object
        agingReportObj = getReportObject(reportName);
        if(!(agingReportObj.size()>0)){
            csAssert.assertFalse(true,"User do not have permission of report "+ reportName);
        }


        try {

            //remove report permission
            Set<String> reportGrantedListForUser = adminHelper.getReportsGrantedListForUser(ConfigureEnvironment.getEnvironmentProperty("j_username"), adminHelper.getClientIdFromDB());
            int reportIdForReportName = adminHelper.getReportIdFromReportName("Obligations - Aging");
            reportGrantedListForUser.remove(String.valueOf(reportIdForReportName));
            adminHelper.updateReportGrantedListForUser(ConfigureEnvironment.getEnvironmentProperty("j_username"), new AdminHelper().getClientIdFromDB(), reportGrantedListForUser.toString().replaceAll("\\[", "{").replaceAll("\\]", "}"));


            //Hit User report renderer Api and get report object
            agingReportObj = getReportObject(reportName);
            if(!(agingReportObj.size()==0)){
                csAssert.assertFalse(true,"Report "+ reportName+" is also visible to user after removing report permission "+reportIdForReportName );
            }

            //add report permission
            Set<String> afterreportGrantedListForUser = adminHelper.getReportsGrantedListForUser(ConfigureEnvironment.getEnvironmentProperty("j_username"), adminHelper.getClientIdFromDB());
            afterreportGrantedListForUser.add(String.valueOf(reportIdForReportName));
            adminHelper.updateReportGrantedListForUser(ConfigureEnvironment.getEnvironmentProperty("j_username"), new AdminHelper().getClientIdFromDB(), afterreportGrantedListForUser.toString().replaceAll("\\[", "{").replaceAll("\\]", "}"));


            //Hit User report renderer Api and get report object
            agingReportObj = getReportObject(reportName);
            if(!(agingReportObj.size()>0)){
                csAssert.assertFalse(true,"Report "+ reportName+" is not visible to user after adding report permission "+reportIdForReportName );
            }

        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while updating permission of report "+ reportName+" " + e.getMessage());
        }

        csAssert.assertAll();

    }


    private JSONArray getReportObject(String reportName){
        reportRenderListReportJsonObj.hitReportRender();
        String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
        JSONArray agingReport = (JSONArray)JSONUtility.parseJson(reportRenderJsonStr,"$[*][?(@.name=='Child Obligations')].listMetaDataJsons[*][?(@.name=='"+reportName+"')]");
        return  agingReport;
    }




   @Test(description = "C8742")
    public void TestEdit(){
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

                    logger.info("Perform Entity Workflow Action For Created Obligation");
                    EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
                    String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
                    for (String actionLabel : workFlowStep) {
                        logger.info(actionLabel);
                        entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, obligationEntityId, actionLabel);
                    }

                    waitForCOBCreation(120000,obligationEntityId);

                    if (validateCOBCreation(obligationEntityId)!=null){
                        show.hitShowGetAPI(obligationEntityTypeId,obligationEntityId);
                        String obligationShow = show.getShowJsonStr();
                        editObligation(obligationShow,csAssert);
                        validateCOBUpdation(csAssert,validateCOBCreation(obligationEntityId));
                    }else{
                        csAssert.assertFalse(true,"COB are not created by schedular");
                    }
                }else{
                    csAssert.assertFalse(true,"obligation is not created successfully");
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



    @Test(description = "C8610")
    public void TestTriggered(){
        int entityId = -1;
        String flowToTest = "flow 12";
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
                    entityId = CreateEntity.getNewEntityId(createResponse, "obligations");

                    logger.info("Perform Entity Workflow Action For Created Obligation");
                    EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
                    String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
                    for (String actionLabel : workFlowStep) {
                        logger.info(actionLabel);
                        entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, entityId, actionLabel);
                    }

                    waitForCOBCreation(120000,entityId);

                    if (validateCOBCreation(entityId)!=null){
                        csAssert.assertFalse(true, "COB created in Obligation "+entityId
                        +" even if triggered checkbbox is checked in obligation");
                    }
                }else{
                    csAssert.assertFalse(true,"obligation is not created successfully");
                }
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && entityId != -1) {
                logger.info("Deleting Obligation having Id {}", entityId);
                EntityOperationsHelper.deleteEntityRecord("obligations", entityId);
            }
            csAssert.assertAll();
        }

    }


    @Test(description = "C8605")
    public void TestWeekType(){
        int entityId = -1;
        String flowToTest = "flow 13";
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
                    entityId = CreateEntity.getNewEntityId(createResponse, "obligations");

                    logger.info("Perform Entity Workflow Action For Created Obligation");
                    EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
                    String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
                    for (String actionLabel : workFlowStep) {
                        logger.info(actionLabel);
                        entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, entityId, actionLabel);
                    }

                    waitForCOBCreation(120000,entityId);
                    ArrayList<String> cobDueDate = new ArrayList<>();
                    ArrayList<String> expectedcobDueDate = new ArrayList<>();
                    expectedcobDueDate.add("04-14-2020");
                    expectedcobDueDate.add("04-13-2020");
                    expectedcobDueDate.add("04-10-2020");
                    JSONArray cobIds = validateCOBCreation(entityId);
                    if (cobIds!=null){

                        for (int i = 0; i <cobIds.size() ; i++) {
                            String cobId = cobIds.get(i).toString().split(":;")[1];
                            show.hitShow(cobEntityTypeId,Integer.parseInt(cobId));
                          String dueDate = (String)JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.dueDate.values");
                          cobDueDate.add(dueDate.split(" ")[0]);
                        }

                       csAssert.assertTrue(cobDueDate.equals(expectedcobDueDate),"cob doesn't shifted to friday ");
                        deleteentities(cobIds);
                    }else{
                        csAssert.assertFalse(true,"COB are not created by schedular");
                    }
                }else{
                    csAssert.assertFalse(true,"obligation is not created successfully");
                }
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && entityId != -1) {
                logger.info("Deleting Obligation having Id {}", entityId);
                EntityOperationsHelper.deleteEntityRecord("obligations", entityId);
            }
            csAssert.assertAll();
        }

    }


    @Test(description = "C8595,C8571")
    public void TestFrequency(){
        int entityId = -1;
        String flowToTest = "flow 14";
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
                    entityId = CreateEntity.getNewEntityId(createResponse, "obligations");

                    logger.info("Perform Entity Workflow Action For Created Obligation");
                    EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
                    String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
                    for (String actionLabel : workFlowStep) {
                        logger.info(actionLabel);
                        entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, entityId, actionLabel);
                    }

                    waitForCOBCreation(240000,entityId);
                    String[] expected = {};
                    ArrayList<String> cobDueDate = new ArrayList<>();
                    ArrayList<String> expectedcobDueDate = new ArrayList(Arrays.asList(expected));
                    expectedcobDueDate.add("08-01-2020");
                    expectedcobDueDate.add("07-04-2020");
                    expectedcobDueDate.add("06-04-2020");
                    expectedcobDueDate.add("05-04-2020");
                    expectedcobDueDate.add("04-04-2020");
                    expectedcobDueDate.add("04-01-2020");
                    JSONArray cobIds = validateCOBCreation(entityId);
                    if (cobIds!=null){

                        for (int i = 0; i <cobIds.size() ; i++) {
                            String cobId = cobIds.get(i).toString().split(":;")[1];
                            show.hitShow(cobEntityTypeId,Integer.parseInt(cobId));
                            String dueDate = (String)JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.dueDate.values");
                            cobDueDate.add(dueDate.split(" ")[0]);
                        }

                        csAssert.assertTrue(cobDueDate.equals(expectedcobDueDate),"cob due date are not according to the frequency Provided ");
                        deleteentities(cobIds);
                    }else{
                        csAssert.assertFalse(true,"COB are not created by schedular");
                    }
                }else{
                    csAssert.assertFalse(true,"obligation is not created successfully");
                }
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && entityId != -1) {
                logger.info("Deleting Obligation having Id {}", entityId);
                EntityOperationsHelper.deleteEntityRecord("obligations", entityId);
            }
            csAssert.assertAll();
        }

    }




    private void waitForCOBCreation(int maxTime, int obligationEntityId) throws InterruptedException {
        int time = 0;
        while (time < maxTime && validateCOBCreation(obligationEntityId)==null){
            Thread.sleep(5000);
            time = time +5000;

        }
    }

    private JSONArray validateCOBCreation( int obligationEntityId){
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


    private void validateCOBUpdation(CustomAssert csAssert, JSONArray cobIds){
        for (int i = 0; i <cobIds.size() ; i++) {
            String  COBID    = cobIds.get(i).toString().split(":;")[1];
            show.hitShowGetAPI(cobEntityTypeId,Integer.valueOf(COBID));
            String cobShow = show.getShowJsonStr();
            csAssert.assertEquals(JSONUtility.parseJson(cobShow,"$.body.data.description.values"),"sarthak","description is not updated in the COB "+COBID);
            if(JSONUtility.parseJson(cobShow,"$.body.data.description.values").equals("sarthak")){
                logger.info("Deleting Obligation having Id {}", COBID);
                EntityOperationsHelper.deleteEntityRecord("child obligations", Integer.parseInt(COBID));
            }
        }
    }




    private void editObligation(String showResponse,CustomAssert csAssert) throws Exception {

        JSONObject obligationdata = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");
        obligationdata.put("description", new JSONObject("{\"name\":\"description\",\"id\":304,\"values\":\"sarthak\",\"multiEntitySupport\":false}"));
        obligationdata.remove("history");

        JSONObject payload = new JSONObject();
        JSONObject body = new JSONObject();
        body.put("data",obligationdata);
        payload.put("body",body);
        String editResponce = edit.hitDnoEdit("obligations",payload.toString());
        csAssert.assertEquals(JSONUtility.parseJson(editResponce,"$.header.response.status"),
                "success", "obligation "+ obligationEntityId+" description is not updated");

    }

    private void deleteentities(JSONArray cobIdArray){
        if (deleteEntity && cobIdArray != null) {
            for (int i = 0; i <cobIdArray.size() ; i++) {
                String  COBID    = cobIdArray.get(i).toString().split(":;")[1];
                logger.info("Deleting Obligation having Id {}", COBID);
                EntityOperationsHelper.deleteEntityRecord("child obligations", Integer.parseInt(COBID));
            }
        }
    }

}
