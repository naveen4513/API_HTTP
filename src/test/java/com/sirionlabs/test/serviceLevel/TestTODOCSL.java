package com.sirionlabs.test.serviceLevel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestTODOCSL extends TestAPIBase {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestTODOCSL.class);

    private Integer slEntityTypeId;
    private String slConfigFilePath;
    private String slConfigFileName;
    private String auditLogUser;
    private String configFilePath = null;
    private String configFileName = null;
    public int beforeCount = 0;
    public int afterCount = 0;
    public int beforeUpcomingCount = 0;
    public int afterUpcomingCount = 0;
    String dateFormat = "MMM-dd-yyyy";

    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    @BeforeClass()
    public void BeforeClass(){
        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        auditLogUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");

    }

    @Test(enabled = true, priority = 0)
    public void TestTODOTodayBeforeCSLCreation(){

        CustomAssert customAssert = new CustomAssert();
        JSONArray responseJson;
        String ApiUrl = "/pending-actions/daily/15/";

        try {

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(ApiUrl, headers).getResponse();
            String responseBody = response.getResponseBody();

            if (!APIUtils.validJsonResponse(responseBody)) {
                logger.error("TODO daily API Response is not a valid Json");
                customAssert.assertTrue(false, "TODO daily API Response is not a valid Json");
            }

            responseBody = response.getResponseBody();

            responseJson = new JSONArray(responseBody);

            for(int i=0; i<responseJson.length();i++) {
                if(responseJson.getJSONObject(i).getString("statusName").equalsIgnoreCase("Overdue")){
                    beforeCount = responseJson.getJSONObject(i).getInt("count");
                    break;
                }

            }
        }catch (Exception e){
            customAssert.assertFalse(false,"Todo today tab not validated");

        }

    }

    @Test(enabled = true, priority = 1)
    public void TestTODOUpcomingBeforeCSLCreation(){

        CustomAssert customAssert = new CustomAssert();
        JSONArray ResponseJson = new JSONArray();
        String newApiUrl = "/pending-actions/weekly/15/";

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(newApiUrl, headers).getResponse();
            String responseBody = response.getResponseBody();

            if (!APIUtils.validJsonResponse(responseBody)) {
                logger.error("New API Response is not a valid Json");
                customAssert.assertTrue(false, "API Response is not a valid Json");
            }

            responseBody = response.getResponseBody();

            ResponseJson = new JSONArray(responseBody);

            for(int i=0; i<ResponseJson.length();i++) {
                if(ResponseJson.getJSONObject(i).getString("statusName").equalsIgnoreCase("Upcoming")){
                    beforeUpcomingCount = ResponseJson.getJSONObject(i).getInt("count");
                    break;
                }

            }
        }catch (Exception e){
            customAssert.assertFalse(false,"Upcoming tab is not validated");
        }

    }

    @Test(enabled = true,  priority = 2)
    public void TestCSLCreation() {

        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        CustomAssert customAssert = new CustomAssert();

        try {
            String flowToTest = "sl automation flow";

            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

            int serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);
            if (serviceLevelId != -1) {

                List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active

                if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, auditLogUser, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    customAssert.assertAll();
                }

            } else {

                customAssert.assertTrue(false, "Service Level Id not created");
                customAssert.assertAll();
                return;
            }
            slToDelete.add(serviceLevelId);

            ArrayList<String> childServiceLevelIdList = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

            addCSLToDelete(childServiceLevelIdList);

            int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

            if (!(childServiceLevelIdList.size() >= numberOfChildServiceLevel)) {
                customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childServiceLevelIdList.size());
                customAssert.assertAll();
            }
        }catch (Exception e){
            customAssert.assertFalse(false,"either SL or CSl has not created");
        }
    }

    @Test(enabled = true, priority = 3)
    public void TestTODOTodayAfterCSLCreation(){

        CustomAssert customAssert = new CustomAssert();
        JSONArray responseJson;
        String newApiUrl = "/pending-actions/daily/15/";

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(newApiUrl, headers).getResponse();
            String responseBody = response.getResponseBody();

            if (!APIUtils.validJsonResponse(responseBody)) {
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false, "API Response is not a valid Json");
            }

            responseBody = response.getResponseBody();
            responseJson = new JSONArray(responseBody);

            for(int i=0; i<responseJson.length();i++) {
                if(responseJson.getJSONObject(i).getString("statusName").equalsIgnoreCase("Report Submitted")){
                    afterCount = responseJson.getJSONObject(i).getInt("count");
                    break;
                }

            }
            if (afterCount>beforeCount){
                logger.debug("ToDO today tab is validated successfully");
            }else{
                customAssert.assertTrue(false,"Overdue count is same before and after data creation");
            }

            Boolean checkDates = checkReportingDate();
            if(!checkDates){
                logger.error("Reporting dates are greater than Today");
            }
        }catch (Exception e){
            logger.error("Todo Today Tab is validated unsuccessfully");
            customAssert.assertFalse(false,"Today tab validated unsuccessfully");
        }
    }

    @Test(enabled = false, priority = 4)
    public void TestTODOUpcomingAfterCSLCreation(){

        CustomAssert customAssert = new CustomAssert();
        JSONArray ResponseJson = new JSONArray();
        String newApiUrl = "/pending-actions/weekly/15/";

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(newApiUrl, headers).getResponse();
            String responseBody = response.getResponseBody();

            if (!APIUtils.validJsonResponse(responseBody)) {
                logger.error("New API Response is not a valid Json");
                customAssert.assertTrue(false, "API Response is not a valid Json");
            }
            responseBody = response.getResponseBody();
            ResponseJson = new JSONArray(responseBody);

            for(int i=0; i<ResponseJson.length();i++) {
                if(ResponseJson.getJSONObject(i).getString("statusName").equalsIgnoreCase("Upcoming")){
                    afterUpcomingCount = ResponseJson.getJSONObject(i).getInt("count");
                    break;
                }
            }
            if (afterUpcomingCount>=beforeUpcomingCount+1){
                logger.debug("ToDO today tab is validated successfully");
            }
            else{
                customAssert.assertTrue(false,"Overdue count is same before and after data creation");
            }

            Boolean checkDates = checkReportingDateUpcoming();
            if(!checkDates){
                logger.error("Reporting dates are greater than Today");
            }
        }catch (Exception e){
            logger.error("Todo Upcoming Tab is validated unsuccessfully");
            customAssert.assertFalse(false,"Upcoming Tab is validated unsuccessfully");
        }

    }

    public boolean checkReportingDate(){

        CustomAssert customAssert = new CustomAssert();
        int listId = 265;
        Boolean checkDates = true;

        String response;
        String payload = "{ \"filterMap\": { \"entityTypeId\": 15, \"offset\":0, \"size\":10000, \"orderByColumnName\": \"id\", \"orderDirection\": \"desc nulls last\", \"filterJson\": {}, \"customFilter\": { \"pendingAction\": { \"occurrence\": \"daily\", \"statusId\": 1623 } }, \"bypassPreferredView\": true }, \"selectedColumns\": [ { \"columnId\": 12140, \"columnQueryName\": \"duedate\" } ] }";
        try{

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListData(listId, true,payload, null);

            response = listRendererListData.getListDataJsonStr();
            JSONObject listDataResponseJSON = new JSONObject(response);
            JSONArray dataArray = listDataResponseJSON.getJSONArray("data");
            String sDate1 = DateUtils.getCurrentDateInMMM_DD_YYYY();
            Date date1=new SimpleDateFormat(dateFormat).parse(sDate1);

            for(int i=0;i<dataArray.length();i++){
                JSONArray data =  dataArray.getJSONObject(i).names();
                for(int j=0;j<data.length();j++){
                    if(dataArray.getJSONObject(i).getJSONObject(data.getString(j)).getString("columnName").equalsIgnoreCase("duedate")){
                        String sDate2 = dataArray.getJSONObject(i).getJSONObject(data.getString(j)).getString("value");
                        Date date2 =new SimpleDateFormat("MMM-dd-yyyy").parse(sDate2);
                        if(date2.compareTo(date1)>0){
                            checkDates = false;
                            logger.error("Reporting Date is greater than Today");
                            customAssert.assertTrue(false,"Reporting Date is greater than Today");
                            break;
                        }
                    }
                }

            }
        }catch (Exception e){
            logger.error("Reporting Dates not validated successfully");
            customAssert.assertTrue(false,"Reporting Dates not validated successfully");

        }
        return checkDates;
    }

    public boolean checkReportingDateUpcoming(){

        CustomAssert customAssert = new CustomAssert();
        int listId = 265;
        Boolean checkDates = true;

        String response;
        String payload = "{ \"filterMap\": { \"entityTypeId\": 15, \"offset\":0, \"size\":10000, \"orderByColumnName\": \"id\", \"orderDirection\": \"desc nulls last\", \"filterJson\": {}, \"customFilter\": { \"pendingAction\": { \"occurrence\": \"daily\", \"statusId\": 1623 } }, \"bypassPreferredView\": true }, \"selectedColumns\": [ { \"columnId\": 12140, \"columnQueryName\": \"duedate\" } ] }";
        try{

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListData(listId, true,payload, null);

            response = listRendererListData.getListDataJsonStr();
            JSONObject listDataResponseJSON = new JSONObject(response);
            JSONArray dataArray = listDataResponseJSON.getJSONArray("data");
            String sDate1 = DateUtils.getCurrentDateInMMM_DD_YYYY();
            Date date1=new SimpleDateFormat(dateFormat).parse(sDate1);

            for(int i=0;i<dataArray.length();i++){
                JSONArray data =  dataArray.getJSONObject(i).names();
                for(int j=0;j<data.length();j++){
                    if(dataArray.getJSONObject(i).getJSONObject(data.getString(j)).getString("columnName").equalsIgnoreCase("duedate")){
                        String sdate2 = dataArray.getJSONObject(i).getJSONObject(data.getString(j)).getString("value");
                        Date date2 =new SimpleDateFormat("MMM-dd-yyyy").parse(sdate2);
                        if(date2.compareTo(date1)<0){
                            checkDates = false;
                            logger.error("Reporting Date is greater than Today");
                            customAssert.assertTrue(false,"Reporting Date is greater than Today");
                        }
                    }
                }

            }
        }catch (Exception e){
            logger.error("Reporting Dates not validated successfully");
            customAssert.assertTrue(false,"Reporting Dates not validated successfully");
        }
        return checkDates;
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

    @AfterClass(groups = {"sanity","sprint"})
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels", cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels", slToDelete);

    }
}