package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCreditandEarnback {

    private final static Logger logger = LoggerFactory.getLogger(TestCreditandEarnback.class);

    private String slConfigFilePath;
    private String slConfigFileName;
    private String creditConfigFilePath;
    private String creditConfigFileName;
    int serviceLevelId;
    ArrayList<String> childServiceLevelIds;
    private String completedBy;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();
    private String cslEntity = "childslas";
    int slEntityTypeId = 14;
    int cslEntityTypeId = 15;

    @BeforeClass
    public void BeforeClass(){

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");
        creditConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreditAndEarnbackConfigFilePath");
        creditConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CreditAndEarnbackConfigFileName");
        completedBy = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "completedby");

    }

    @DataProvider(name = "flowsToTest", parallel = false)
    public Object[][] flowsToTest() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(creditConfigFilePath,creditConfigFileName,"flowstotest").split(",");

        for(String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "flowsToTest",enabled = false)
    public void TestCreditandEarnbackFixedAMount(String dataToTest){
        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl credit and earnback fixed amount";
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        int cslId = -1;
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}\", \"init_script\": \"state['map'] =['met': 0, 'notMet': 0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':1, 'Final_Denominator':2,'Actual_Numerator':10, 'Actual_Denominator':100]; for (a in states){params.return_map.Final_Numerator +=(float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet);} if(params.return_map.Final_Denominator > 447){params.return_map.Final_Numerator = 95; params.return_map.Final_Denominator = 100;}return params.return_map\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
        String DCQ = "";
        String flow = ParseConfigFile.getValueFromConfigFile(creditConfigFilePath,creditConfigFileName,dataToTest,"flow");
        int value = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(creditConfigFilePath,creditConfigFileName,dataToTest,"values"));
        String actionNameAuditLog = ParseConfigFile.getValueFromConfigFile(creditConfigFilePath,creditConfigFileName,dataToTest,"action");;
        String slMetName = ParseConfigFile.getValueFromConfigFile(creditConfigFilePath,creditConfigFileName,dataToTest,"metname");
        int slMetId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(creditConfigFilePath,creditConfigFileName,dataToTest,"slmetid"));

        try {

//            serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);
            serviceLevelId = 21134;
            if (serviceLevelId != -1) {
                customAssert.assertFalse(false, "Service Level has been created successfully" + serviceLevelId);
                slToDelete.add(serviceLevelId);
            }

            else {
                customAssert.assertFalse(true, "Service Level has not created successfully" + serviceLevelId);
            }
//            List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
//            //Performing workflow Actions till Active
//
//            if(!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps,"", customAssert)) {
//                customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
//            }

            childServiceLevelIds = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

            addCSLToDelete(childServiceLevelIds);

            int numberOfCSLToUpdate = 2;

            for (int i=0;i<numberOfCSLToUpdate;i++){

                cslId = Integer.parseInt(childServiceLevelIds.get(5));
                if(!updateSLMetStatusValues(cslId,slMetName,slMetId,customAssert)){
                    customAssert.assertTrue(false, "SL met Values updated unsuccessfully for CSL ID " + cslId);
                }

                List<String> cslWorkFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "creditandearnbackworkflowsteps").split("->"));

                //Performing workflow Actions till Error in computation
                if (!performWorkFlowActions(cslEntityTypeId, cslId, cslWorkFlowSteps, completedBy, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on CSL ID " + cslId);
                }

                Thread.sleep(6000);

                if(!updateCalculatedAmount(cslId,value,flow,customAssert)){
                    customAssert.assertTrue(false, "Error while updating Calculated Credit/Earnback Amount " + cslId);
                }

                String showPageResponse;
                Show show = new Show();
                show.hitShowVersion2(cslEntityTypeId, cslId);
                showPageResponse = show.getShowJsonStr();

                if(flow.equals("credit")) {
                    int creditValue = Integer.parseInt(ShowHelper.getValueOfField("creditValue", showPageResponse));
                    if (!verifyAuditLog(cslEntityTypeId, cslId, actionNameAuditLog, completedBy, customAssert)) {
                        customAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + cslId);
                    }

                }
                else if (flow.equals("earnback")){
                    int earnbackValue = Integer.parseInt(ShowHelper.getValueOfField("earnbackValue", showPageResponse));
                    if (!verifyAuditLog(cslEntityTypeId, cslId, actionNameAuditLog, completedBy, customAssert)) {
                        customAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + cslId);
                    }
                }

            }

        }catch (Exception e){
            customAssert.assertFalse(true,"Exception while validating the Credit and Earnback Fixed Amount Scenario");
        }
        customAssert.assertAll();
    }

    private Boolean updateSLMetStatusValues(int cslId,String slMetName,int slMetId,CustomAssert customAssert){

        logger.info("Updating SL Met Status values");
        Edit edit = new Edit();
        Boolean updateStatus = true;

        try {
            JSONObject editResponseJson = editPayloadJson(cslEntity,cslId);


//            String editResponse = edit.hitEdit(cslEntity, cslId);

//            editResponseJson.remove("header");
//            editResponseJson.remove("session");
//            editResponseJson.remove("actions");
//            editResponseJson.remove("createLinks");
//            editResponseJson.getJSONObject("body").remove("layoutInfo");
//            editResponseJson.getJSONObject("body").remove("globalData");
//            editResponseJson.getJSONObject("body").remove("errors");
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slMet").getJSONObject("values").put("id", slMetId);

            String payload ="{\"body\":{\"data\":{\"slMet\":{\"name\":\"slMet\",\"id\":1151,\"values\":{\"name\":\""+slMetName+"\",\"id\":"+slMetId+",\"active\":true}}}}}";
cslEntity = "childslas";
             String editResponse = edit.hitEdit(cslEntity, payload);


            if (!editResponse.contains("success")) {
                customAssert.assertTrue(false, "Child service level updated unsuccessfully");
            }
        }catch (Exception e){
            customAssert.assertTrue(false, "Exception while updating SL Met values on CSL ID " + cslId);
            updateStatus = false;
        }
        return updateStatus;
    }

    private Boolean updateCalculatedAmount(int cslId,int values,String flow,CustomAssert customAssert) {

        logger.info("Updating Calculated Credit/Earnback Amount Values");
        Edit edit = new Edit();
        Boolean updateStatus = true;

        try {
            String editResponse = edit.hitEdit(cslEntity, cslId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");
            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");
            if(flow.equals("credit")){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("calculatedCreditAmount").put("values",values);
            }
            else if(flow.equals("earnback")){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("calculatedEarnbackAmount").put("values",values);

            }
            editResponse = edit.hitEdit(cslEntity, editResponseJson.toString());

            if (!editResponse.contains("success")) {
                customAssert.assertTrue(false, "Child service level updated unsuccessfully");
            }
        }catch (Exception e){
            customAssert.assertTrue(false, "Exception while updating SL Met values on CSL ID " + cslId);
            updateStatus = false;
        }
        return updateStatus;
    }

    private Boolean performWorkFlowActions(int entityTypeId, int entityId, List<String> workFlowSteps, String user, CustomAssert customAssert) {

        Boolean workFlowStepActionStatus;
        String actionNameAuditLog;
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {
            for (String workFlowStepToBePerformed : workFlowSteps) {

                //workFlowStepActionStatus = workflowActionsHelper.performWorkflowAction(entityTypeId, entityId, workFlowStepToBePerformed);
                workFlowStepActionStatus = workflowActionsHelper.performWorkFlowStepV2(entityTypeId,entityId,workFlowStepToBePerformed,customAssert);

                if (!workFlowStepActionStatus) {

                    customAssert.assertTrue(false, "Unable to perform workflow action " + workFlowStepToBePerformed + " on service level id " + entityId);
                    return false;
                } else {
                    actionNameAuditLog = ParseConfigFile.getValueFromConfigFileCaseSensitive(slConfigFilePath, slConfigFileName, "auditlogactioname", workFlowStepToBePerformed);
                    if (!verifyAuditLog(entityTypeId, entityId, actionNameAuditLog, user, customAssert)) {
                        customAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + entityId);
                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getMessage());
            return false;

        }
        return true;
    }

    private Boolean verifyAuditLog(int entityTypeId, int entityId, String actionNameExpected, String user, CustomAssert customAssert) {

        logger.info("Validating Audit Log Tab for entity type id " + entityTypeId + " and entity id " + entityId);

        int AuditLogTabId = 61;

        Boolean validationStatus = true;
//		int expectedValidationChecksOnAuditLogTab = 5;
        int expectedValidationChecksOnAuditLogTab = 3;
        int actualValidationChecksOnAuditLogTab = 0;

        TabListData tabListData = new TabListData();

        JSONObject latestActionRow;
        JSONObject tabListDataResponseJson;
        JSONArray dataArray;
        JSONArray latestActionRowJsonArray;

        String tabListDataResponse;
        String columnName;
        String columnValue;

        try {

            tabListData.hitTabListData(AuditLogTabId, entityTypeId, entityId);
            tabListDataResponse = tabListData.getTabListDataResponseStr();

            if (APIUtils.validJsonResponse(tabListDataResponse)) {

                tabListDataResponseJson = new JSONObject(tabListDataResponse);
                dataArray = tabListDataResponseJson.getJSONArray("data");

                if(dataArray.length() == 0){
                    customAssert.assertTrue(false,"No entry exists in Audit Log Tab for entity id " + entityId + " entity type id " + entityTypeId);
                    return false;
                }

                latestActionRow = dataArray.getJSONObject(dataArray.length() - 1);
                latestActionRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(latestActionRow);

                for (int i = 0; i < latestActionRowJsonArray.length(); i++) {

                    columnName = latestActionRowJsonArray.getJSONObject(i).get("columnName").toString();
                    columnValue = latestActionRowJsonArray.getJSONObject(i).get("value").toString();

                    switch (columnName) {

                        case "action_name":

                            if (!columnValue.equalsIgnoreCase(actionNameExpected)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab action_name is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected action_name : " + actionNameExpected + " Actual action_name : " + columnValue);
                                validationStatus = false;
                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "requested_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab requested_by is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected requested_by : " + actionNameExpected + " Actual requested_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "completed_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab completed_by is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected completed_by : " + actionNameExpected + " Actual completed_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;
                    }
                }

                if (actualValidationChecksOnAuditLogTab == expectedValidationChecksOnAuditLogTab) {
                    customAssert.assertTrue(true, "All validation checks passed successfully");
                } else {
                    customAssert.assertTrue(false, "Validation check count not equal to " + expectedValidationChecksOnAuditLogTab);
                    validationStatus = false;
                }
            } else {
                customAssert.assertTrue(false, "Audit Log Tab Response is not a valid json for entity id " + entityId);
                validationStatus = false;
            }


        } catch (Exception e) {
            logger.error("Exception while validating tab list response " + e.getMessage());
            customAssert.assertTrue(false, "Exception while validating tab list response " + e.getMessage());
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

    private JSONObject editPayloadJson(String entityName, int entityId) {

        Edit edit = new Edit();
        JSONObject editResponseJson = null;
        try {
            String editResponse = edit.hitEdit(entityName, entityId);

            if (APIUtils.validJsonResponse(editResponse)) {

                editResponseJson = new JSONObject(editResponse);

                editResponseJson.remove("header");
                editResponseJson.remove("session");
                editResponseJson.remove("actions");
                editResponseJson.remove("createLinks");
                editResponseJson.getJSONObject("body").remove("layoutInfo");
                editResponseJson.getJSONObject("body").remove("globalData");
                editResponseJson.getJSONObject("body").remove("errors");

            }
        } catch (Exception e) {
            logger.error("Exception while getting edit response for entity " + entityId);
        }
        return editResponseJson;
    }

    @AfterClass
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
    }

}
