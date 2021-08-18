package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class test_SIR217932 {

    private String configFilePath;
    private String configFileName;

    private String slConfigFilePath;
    private String slConfigFileName;

    private int slEntityTypeId;
    private int cslEntityTypeId;

    private String auditLogUser;
    private String adminUser;

    private String cslEntity = "child service levels";

    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    private final static Logger logger = LoggerFactory.getLogger(test_SIR217932.class);

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("testSIR217932ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("testSIR217932ConfigFileName");

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");

        auditLogUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");
        adminUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "adminuser");

    }

    @DataProvider(name = "getFlowsForCreditValuesCalculation", parallel = true)
    public Object[][] getFlowsForCreditValuesCalculation(){

        String[] slValidationFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slvalidationflowsforcreditcalculation").split(",");
        List<Object[]> allTestData = new ArrayList<>();

        for(String flowsToTest : slValidationFlows){
            allTestData.add(new Object[]{flowsToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

//    C88352
    @Test(dataProvider = "getFlowsForCreditValuesCalculation")
    public void TestCreditEarnBackCalculations(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        List<String> workFlowSteps;

        String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
        int serviceLevelId;
        ArrayList<String> childServiceLevelIdList;

        Edit edit = new Edit();

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        String workFlowActionName = "StartComputation";
        String editResponse;
        String editPayload;

        HashMap<String,String> fieldToUpdateMap;

        int cslIdOnWhichCreditIsCalculated = -1;
        int cslIdOnWhichEarnBackIsCalculated = -1;
        String earnBackToBeCalculated;

        try {
            serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);
            slToDelete.add(serviceLevelId);

            if(serviceLevelId !=-1) {
                workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active

                if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, auditLogUser, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    customAssert.assertAll();
                }

                childServiceLevelIdList = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);
                addCSLToDelete(childServiceLevelIdList);

                int cslToStartFrom = 3;
                int totalNumberOfCslToUpdate = 1;

                try {
                    totalNumberOfCslToUpdate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "numberofcsltoupdate"));
                } catch (Exception e) {
                    logger.error("Exception while getting totalNumberOfCslToUpdate for the flow " + flowToTest);
                }
                String creditToBeCalculated = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "credittobecalculated");

                if (creditToBeCalculated.equalsIgnoreCase("true")) {
                    for (int i = cslToStartFrom; i < cslToStartFrom + totalNumberOfCslToUpdate; i++) {

                        cslIdOnWhichCreditIsCalculated = Integer.parseInt(childServiceLevelIdList.get(i));
//                int cslIdOnWhichCreditIsCalculated = 34098;

                        fieldToUpdateMap = createFieldsToUpdateMapForCredit(flowToTest);
                        editResponse = edit.hitEdit(cslEntity, cslIdOnWhichCreditIsCalculated);

                        if (APIUtils.validJsonResponse(editResponse)) {

                            editPayload = createEditPayloadForCSLCreditCalc(editResponse, fieldToUpdateMap);
                            editResponse = edit.hitEdit(cslEntity, editPayload);

                            if (!editResponse.contains("success")) {
                                customAssert.assertTrue(false, "Edit done unsuccessfully for CSL ID For Credit" + cslIdOnWhichCreditIsCalculated);
                            }
                        }
                    }

                    if (workflowActionsHelper.performWorkflowAction(cslEntityTypeId, cslIdOnWhichCreditIsCalculated, workFlowActionName)) {
                        Thread.sleep(10000);
                        if (!validateCreditCalculation(cslIdOnWhichCreditIsCalculated, flowToTest, customAssert)) {

                            customAssert.assertTrue(false, "Credit Values validated unsuccessfully after performing computation");
//                            customAssert.assertAll();
                        }
                    }
                }

                int earnBackStartCslNumber = cslToStartFrom + totalNumberOfCslToUpdate;

//                for(int i =earnBackStartCslNumber;i<earnBackStartCslNumber + totalNumberOfCslToUpdate;i++) {
//                cslIdOnWhichEarnBackIsCalculated = Integer.parseInt(childServiceLevelIdList.get(i));
                cslIdOnWhichEarnBackIsCalculated = Integer.parseInt(childServiceLevelIdList.get(earnBackStartCslNumber));

                fieldToUpdateMap = createFieldsToUpdateMapForEarnBack(flowToTest);
                editResponse = edit.hitEdit(cslEntity, cslIdOnWhichEarnBackIsCalculated);
                editPayload = createEditPayloadForCSLCreditCalc(editResponse, fieldToUpdateMap);
                editResponse = edit.hitEdit(cslEntity, editPayload);

                if (!editResponse.contains("success")) {

                    customAssert.assertTrue(false, "Edit done unsuccessfully for CSL ID For EarnBack" + cslIdOnWhichEarnBackIsCalculated);
                }
                if (!workflowActionsHelper.performWorkflowAction(cslEntityTypeId, cslIdOnWhichEarnBackIsCalculated, workFlowActionName)) {
                    customAssert.assertTrue(false, "Unable to perform " + workFlowActionName + " on CSL ID For EarnBack " + cslIdOnWhichEarnBackIsCalculated);
                } else {

                    earnBackToBeCalculated = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "earnbacktobecalculated");

                    if (earnBackToBeCalculated.equalsIgnoreCase("true")) {

                        Boolean earnBackApplicableStatus = checkEarnBackApplicableStatus(cslIdOnWhichEarnBackIsCalculated);
                        if (earnBackApplicableStatus) {

                            if (!validateEarnBackCalculation(cslIdOnWhichEarnBackIsCalculated, flowToTest, earnBackToBeCalculated, customAssert)) {
                                customAssert.assertTrue(false, "EarnBack Calculation Validated unsuccessfully for CSL " + cslIdOnWhichEarnBackIsCalculated);
                            }
                        } else {
                            customAssert.assertTrue(false, "EarnBack Status not true for CSL ID " + cslIdOnWhichEarnBackIsCalculated);
                        }

                    } else {
                        if (!validateEarnBackCalculation(cslIdOnWhichEarnBackIsCalculated, flowToTest, earnBackToBeCalculated, customAssert)) {
                            customAssert.assertTrue(false, "EarnBack Calculation Validated unsuccessfully for CSL " + cslIdOnWhichEarnBackIsCalculated);
                        }
                    }
                }
//                }
            }else {
                customAssert.assertTrue(false, "Unable to get Service Level id for the flow " + flowToTest);
            }
        }catch (Exception e){
            logger.error("Exception while validating credit values test");
        }
        customAssert.assertAll();
    }

    @AfterClass
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
    }

    private String createEditPayloadForCSLCreditCalc(String editResponse, HashMap<String,String> editMap){

        String editPayload = "";
        JSONObject editResponseJson;
        try{

            editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");
            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            String fieldToUpdate;
            String fieldValue;
            JSONObject valueJson;
            for (Map.Entry<String,String> entry : editMap.entrySet()){
                fieldToUpdate = entry.getKey();
                fieldValue = entry.getValue();
                if(fieldToUpdate.equalsIgnoreCase("slMet")){

                    valueJson = new JSONObject(fieldValue);

                    editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject(fieldToUpdate).put("values",valueJson);

                }else {
                    editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject(fieldToUpdate).put("values",fieldValue);
                }
            }
            editPayload = editResponseJson.toString();
        }catch (Exception e){
            logger.error("Exception while creating Edit Payload for CSL Credit Calculation " + e.getMessage());
        }

        return editPayload;
    }

    private HashMap<String,String> createFieldsToUpdateMapForCredit(String flowToTest){

        HashMap<String,String> fieldsToUpdateMap= new HashMap<>();
        String fieldValue;
        try {
            String[] fieldsToUpdateArray = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "fieldstoupdatecredit").split(",");

            for (String fieldToUpdate : fieldsToUpdateArray) {

                fieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,fieldToUpdate.toLowerCase() + "credit");
                fieldsToUpdateMap.put(fieldToUpdate,fieldValue);

            }
        }catch (Exception e){
            logger.error("Exception while creating Fields To Update Map " + e.getMessage());
        }
        return fieldsToUpdateMap;
    }

    private HashMap<String,String> createFieldsToUpdateMapForEarnBack(String flowToTest){

        HashMap<String,String> fieldsToUpdateMap= new HashMap<>();
        String fieldValue;
        try {
            String[] fieldsToUpdateArray = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "fieldstoupdateearnback").split(",");

            for (String fieldToUpdate : fieldsToUpdateArray) {

                fieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,fieldToUpdate.toLowerCase() + "earnback");
                fieldsToUpdateMap.put(fieldToUpdate,fieldValue);

            }
        }catch (Exception e){
            logger.error("Exception while creating Fields To Update Map " + e.getMessage());
        }
        return fieldsToUpdateMap;
    }

    private Boolean checkEarnBackApplicableStatus(int cslIdOnWhichEarnBackIsCalculated){

        Boolean earnBackApplicableStatus = false;
        String sqlQuery = "select system_earnback_applicable_status from child_sla where ID = '" + cslIdOnWhichEarnBackIsCalculated + "'";
        List<List<String>> sqlOutputList;
        String earnBack_applicable_status;
        Long earnBackApplicableTimeOut = 600000L;
        Long pollingTime = 10000L;
        Long startTime = 0L;

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        try {
            while(startTime < earnBackApplicableTimeOut){
                if(earnBackApplicableStatus == true){
                    break;
                }
                Thread.sleep(pollingTime);
                sqlOutputList = postgreSQLJDBC.doSelect(sqlQuery);

                if(sqlOutputList.size() > 0) {
                    if(sqlOutputList.get(0).size() > 0) {
                        earnBack_applicable_status = sqlOutputList.get(0).get(0);
                        if(earnBack_applicable_status != null) {

                            if (earnBack_applicable_status.equalsIgnoreCase("0")) {
                                earnBackApplicableStatus = false;
                            } else if (earnBack_applicable_status.equalsIgnoreCase("1")) {
                                earnBackApplicableStatus = true;
                            }
                        }
                    }
                }
                startTime += pollingTime;

            }
        }catch (Exception e){
            logger.error("Exception while getting earnBack_applicable_status from table child_sla");
            earnBackApplicableStatus = false;
        }

        return earnBackApplicableStatus;
    }

    private Boolean validateCreditCalculation(int cslIdOnWhichCreditIsCalculated,String flowToTest,CustomAssert customAssert){

        Boolean validationStatus = true;

        Show show = new Show();
        show.hitShow(cslEntityTypeId,cslIdOnWhichCreditIsCalculated,true);
        String showResponse = show.getShowJsonStr();

        String creditClauseNameActual = ShowHelper.getValueOfField("creditclausename",showResponse);
        String creditClauseNameExpected = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"creditclausename");

        if(!creditClauseNameActual.equalsIgnoreCase(creditClauseNameExpected)){
            customAssert.assertTrue(false,"Expected and Actual Credit Clause Name are not equal");
            validationStatus = false;
        }

        String creditEarnBackAppliedActual = ShowHelper.getValueOfField("creditearnbackapplied",showResponse);
        String creditEarnBackAppliedExpected = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"creditearnbackapplied");

        if(!creditEarnBackAppliedActual.equalsIgnoreCase(creditEarnBackAppliedExpected)){
            customAssert.assertTrue(false,"Expected and Actual credit EarnBack Applied are not equal");
            validationStatus = false;
        }
        return validationStatus;
    }

    private Boolean validateEarnBackCalculation(int cslIdOnWhichEarnBackIsCalculated,String flowToTest,String earnBackToBeCalculated,CustomAssert customAssert){

        Boolean validationStatus = true;

        String earnBackAppliedMsgActual;
        String earnBackAppliedMsgExpected;
        String earnBackApplicable;
        String earnBackClauseNameActual;
        String earnBackClauseNameExpected;
        String creditEarnBackAppliedActual;

        String earnBackValueActual;
        String earnBackValueExpected;

        Show show = new Show();
        show.hitShow(cslEntityTypeId, cslIdOnWhichEarnBackIsCalculated, true);
        String showResponse = show.getShowJsonStr();

        JSONObject showResponseJson = new JSONObject(showResponse);

        creditEarnBackAppliedActual = ShowHelper.getValueOfField("creditearnbackapplied", showResponse);
        String earnBackAppliedExpectedValue =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "earnbackapplied");

        if(earnBackToBeCalculated.equalsIgnoreCase("false")){

            if (!creditEarnBackAppliedActual.equalsIgnoreCase(earnBackAppliedExpectedValue)) {
                customAssert.assertTrue(false, "Expected and Actual credit EarnBack Applied are not equal");
                validationStatus = false;
                return validationStatus;
            }
            return validationStatus;
        }

        if (!creditEarnBackAppliedActual.equalsIgnoreCase(earnBackAppliedExpectedValue)) {
            customAssert.assertTrue(false, "Expected and Actual credit EarnBack Applied are not equal");
            validationStatus = false;
            return validationStatus;
        }

        earnBackAppliedMsgActual = showResponseJson.getJSONObject("body").getJSONObject("errors").get("warnings").toString();
        earnBackAppliedMsgExpected = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"earnbackappliedmsgexpected");

        if(!earnBackAppliedMsgActual.equalsIgnoreCase(earnBackAppliedMsgExpected)){
            customAssert.assertTrue(false,"Expected message during earnBack calculation is not as per expected");
            validationStatus = false;
        }

        earnBackApplicable = ShowHelper.getValueOfField("earnbackapplicable", showResponse);

        if(!earnBackApplicable.equalsIgnoreCase("true")){
            customAssert.assertTrue(false,"Expected and Actual Value for EarnBack Applicable is not as per expected for CSL ID " + cslIdOnWhichEarnBackIsCalculated);
            validationStatus = false;
        }

        earnBackClauseNameActual = ShowHelper.getValueOfField("earnbackclausename", showResponse);
        earnBackClauseNameExpected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "earnbackclausename");

        if (!earnBackClauseNameActual.equalsIgnoreCase(earnBackClauseNameExpected)) {
            customAssert.assertTrue(false, "Expected and Actual EarnBack Clause Name are not equal");
            validationStatus = false;
        }

        earnBackValueActual = ShowHelper.getValueOfField("earnbackvalue", showResponse);
        earnBackValueExpected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "earnbackvalue");

        if (!earnBackValueActual.equalsIgnoreCase(earnBackValueExpected)) {
            customAssert.assertTrue(false, "Expected and Actual credit EarnBack Applied Value are not equal");
            validationStatus = false;
        }

        return validationStatus;
    }

    private void addCSLToDelete(ArrayList<String> cslToDeleteList){

        try {
            for (String cslIDToDelete : cslToDeleteList) {
                cslToDelete.add(Integer.parseInt(cslIDToDelete));
            }
        }catch (Exception e){
            logger.error("Error while adding child service level to deleted list");
        }
    }
}
