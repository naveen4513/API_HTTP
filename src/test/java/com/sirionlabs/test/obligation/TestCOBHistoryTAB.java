package com.sirionlabs.test.obligation;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestCOBHistoryTAB {
    private final static Logger logger = LoggerFactory.getLogger(TestCOBHistoryTAB.class);

    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer obligationEntityTypeId;
    private static Integer obligationEntityId;
    private static Boolean deleteEntity = true;
    private  DefaultUserListMetadataHelper  metadataHelper;
    private ListRendererTabListData tabData;
    private int cobEntityTypeId;
    private  HashMap<String,String> metadataparam;
    private int pastCOBListId = 70;
    private int futureCOBListId = 71;
    private int childObligationTabId = 60;
    private int cobEntityId;
    private HashMap<String, Integer> pastCOBMetaMap;
    private HashMap<String, Integer> futureCOBMetaMap;
    private HashMap<String, String> pastCOBDataMap;
    private HashMap<String, String> futureCOBDataMap;
    private Show show;

    @BeforeClass
   public void before(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

        metadataHelper = new DefaultUserListMetadataHelper();
        tabData = new ListRendererTabListData();
        cobEntityTypeId = ConfigureConstantFields.getEntityIdByName("child obligations");
        metadataparam = new HashMap<>();
        metadataparam.put("entityTypeId",String.valueOf(cobEntityTypeId));
        pastCOBMetaMap = new HashMap<>();
        futureCOBMetaMap = new HashMap<>();
        pastCOBDataMap = new HashMap<>();
        futureCOBDataMap = new HashMap<>();
        show = new Show();

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
}




    @Test(description = "C9063,C9064")
    public void TestCOBHistoryTAB() throws ParseException, InterruptedException {

        CustomAssert csassert = new CustomAssert();

        obligationEntityId = createObligation("flow 11",csassert);
        workflowActionOnObligation(obligationEntityId);
        waitForCOBCreation(120000);
        JSONArray cobArray = validateCOBCreation();
        if(cobArray !=null){

            cobEntityId = Integer.valueOf(cobArray.get(cobArray.size()/2).toString().split(":;")[1]);


        String pastCOBMetaResponse = metadataHelper.getDefaultUserListMetadataResponse(pastCOBListId,metadataparam);


        JSONArray pastId_array = (JSONArray)JSONUtility.parseJson(pastCOBMetaResponse,"$.columns[*].id");
        JSONArray pastQueryName_array = (JSONArray)JSONUtility.parseJson(pastCOBMetaResponse,"$.columns[*].queryName");


        for (int i = 0; i <pastId_array.size() ; i++) {
            pastCOBMetaMap.put((String)pastQueryName_array.get(i),(Integer)pastId_array.get(i));
        }

        String futureCOBMetaResponse = metadataHelper.getDefaultUserListMetadataResponse(futureCOBListId,metadataparam);

        JSONArray futureId_array = (JSONArray)JSONUtility.parseJson(futureCOBMetaResponse,"$.columns[*].id");
        JSONArray futureQueryName_array = (JSONArray)JSONUtility.parseJson(futureCOBMetaResponse,"$.columns[*].queryName");


        for (int i = 0; i <futureId_array.size() ; i++) {
            futureCOBMetaMap.put((String)futureQueryName_array.get(i),(Integer)futureId_array.get(i));
        }



        String payload = "{\"filterMap\":{\"entityTypeId\":13,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

        tabData.hitListRendererTabListData(pastCOBListId,cobEntityTypeId,cobEntityId,payload);
        String  pastCOBDataResponse = tabData.getTabListDataJsonStr();
        JSONArray data =  (JSONArray)JSONUtility.parseJson(pastCOBDataResponse,"$.data");
        for (int i = 0; i <data.size() ; i++) {
          String[] id = ((LinkedHashMap) ((LinkedHashMap) data.get(i)).get(pastCOBMetaMap.get("id").toString())).get("value").toString().split(":;");
          String due_date = ((LinkedHashMap) ((LinkedHashMap) data.get(i)).get(pastCOBMetaMap.get("due_date").toString())).get("value").toString();
          pastCOBDataMap.put(id[1],due_date);
        }


        tabData.hitListRendererTabListData(futureCOBListId,cobEntityTypeId,cobEntityId,payload);
        String  futureCOBDataResponse = tabData.getTabListDataJsonStr();
        JSONArray future_data =  (JSONArray)JSONUtility.parseJson(futureCOBDataResponse,"$.data");
        for (int i = 0; i <future_data.size() ; i++) {
            String[] id = ((LinkedHashMap) ((LinkedHashMap) future_data.get(i)).get(futureCOBMetaMap.get("id").toString())).get("value").toString().split(":;");
            String due_date = ((LinkedHashMap) ((LinkedHashMap) future_data.get(i)).get(futureCOBMetaMap.get("due_date").toString())).get("value").toString();
            futureCOBDataMap.put(id[1],due_date);
        }


        show.hitShow(cobEntityTypeId,cobEntityId);
        String currentCOBDueDate = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.dueDate.displayValues");

        validatePastCOBDueDate(currentCOBDueDate,pastCOBDataMap,csassert);
        validateFutureCOBDueDate(currentCOBDueDate,futureCOBDataMap,csassert);
        }else {
            csassert.assertFalse(true,"COB are not created by schedular");
        }
        csassert.assertAll();
       deleteentities(cobArray,obligationEntityId);
    }



private boolean validatePastCOBDueDate(String currentCOBDueDate,  HashMap<String,String> pastCOBDataMap, CustomAssert csassert) throws ParseException {
    boolean result = true;
    SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy");
    Date date1 = sdf.parse(currentCOBDueDate);
    for (Map.Entry<String, String>  entry :pastCOBDataMap.entrySet()) {
        Date date2 = sdf.parse(entry.getValue());
        logger.debug("current COB Due date : " + sdf.format(date1));
        logger.debug("Past COD Due date : " + sdf.format(date2));

        if(!(date1.compareTo(date2) > 0)){
            result = false;
        logger.info("Due date of PAST COB: "+entry.getKey()+" is: "+ date2);
        logger.info("Due date of current COB: "+ cobEntityId+" is: "+ date1);
        csassert.assertFalse(true," Due date validation failed for past COB  TAB for COB: "+ cobEntityId);
            break;
        }

    }

    return  result;
}

    private boolean validateFutureCOBDueDate(String currentCOBDueDate,  HashMap<String,String> futureCOBDataMap, CustomAssert csassert) throws ParseException {
        boolean result = true;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy");
        Date date1 = sdf.parse(currentCOBDueDate);
        for (Map.Entry<String, String>  entry :futureCOBDataMap.entrySet()) {
            Date date2 = sdf.parse(entry.getValue());
            logger.debug("current COB Due date : " + sdf.format(date1));
            logger.debug("Future COB Due date : " + sdf.format(date2));

            if(!(date1.compareTo(date2) < 0)){
                result = false;
                logger.info("Due date of Future COB: "+entry.getKey()+" is: "+ date2);
                logger.info("Due date of current COB: "+ cobEntityId+" is: "+ date1);
                csassert.assertFalse(true," Due date validation failed for Future COB  TAB for COB: "+ cobEntityId);
                break;
            }

        }

        return  result;
    }



    private int createObligation(String flowToTest,CustomAssert csAssert){
        int EntityId = -1;
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
                    EntityId = CreateEntity.getNewEntityId(createResponse, "obligations");}
            }else {
                csAssert.assertFalse(true,"Obligation create response is not valid json");
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        }


        return  EntityId;
    }

    private void workflowActionOnObligation(int obligationEntityId){
        logger.info("Perform Entity Workflow Action For Created Obligation");
        EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
        String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
        for (String actionLabel : workFlowStep) {
            logger.info(actionLabel);
            entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, obligationEntityId, actionLabel);
        }
    }


    private void waitForCOBCreation(int maxTime) throws InterruptedException {
        int time = 0;
        while (time < maxTime && validateCOBCreation()==null){
            Thread.sleep(5000);
            time = time +5000;

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


    private void deleteentities(JSONArray cobIdArray , int obligationEntityId){
        if (deleteEntity && cobIdArray != null) {
            for (int i = 0; i <cobIdArray.size() ; i++) {
                String  COBID    = cobIdArray.get(i).toString().split(":;")[1];
                logger.info("Deleting Obligation having Id {}", COBID);
                EntityOperationsHelper.deleteEntityRecord("child obligations", Integer.parseInt(COBID));
            }
        }

        if (deleteEntity && obligationEntityId != -1) {
            logger.info("Deleting Obligation having Id {}", obligationEntityId);
            EntityOperationsHelper.deleteEntityRecord("obligations", obligationEntityId);
        }
    }

}