package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestServiceLevelFrequencyModuleTestCases {

    private final static Logger logger = LoggerFactory.getLogger(TestServiceLevelFrequencyModuleTestCases.class);

    private String slCreationConfigFilePath = null;
    private String slCreationConfigFileName = null;
    private String extraFieldsConfigFilePath = null;
    private String extraFieldsConfigFileName = null;
    private String configFilePath = null;
    private String configFileName = null;

    private String slEntity = "service levels";
    private String cslEntity = "child service levels";

    private int slEntityTypeId;
    private int cslEntityTypeId;

    private final int childServiceLevelTabId = 7;

    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    @BeforeClass
    public void BeforeClass(){

        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        slCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFileName");

        extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsExtraFieldsFileName");


    }


    @DataProvider(name = "frequencyModuleFlowsToValidate", parallel = false)
    public Object[][] frequencyModuleFlowsToValidate() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "frequency module flows").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    // dev bug :
    @Test(dataProvider = "frequencyModuleFlowsToValidate", enabled = true)
    public void TestFrequencyModuleTests(String flowToTest){

        CustomAssert customAssert = new CustomAssert();
        String message = null;

        try{
            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

            int serviceLevelId = getActiveServiceLevelId(message,flowToTest,PCQ,DCQ,customAssert);
            slToDelete.add(serviceLevelId);

            ArrayList<String> childServiceLevelIds = checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);
            addCSLToDelete(childServiceLevelIds);

            String expectedServiceDate = "31-Jan-2019";
            int serviceDateStartingMonth = 1;
            String serviceDateStartingMonthString;
            int serviceDateStartingYear = 2019;

            String expectedReportingDate = "10-Feb-2019";
            int reportingDateStartingMonth = 2;
            int reportingDateStartingDay = 10;
            String reportingDateStartingMonthString;
            String serviceDateStartingDate;
            int reportingDateStartingYear = 2019;

            int cslId;

            int weekType = getWeekType(flowToTest);
            Boolean validationStatus;
            for(int i =0;i<childServiceLevelIds.size();i++){

                cslId = Integer.parseInt(childServiceLevelIds.get(i));

                validationStatus = checkReportingDateAndServiceDateAccToWeekType(message,cslId,weekType,expectedServiceDate,expectedReportingDate,customAssert);

                if(!validationStatus){
                    message = "Reporting Date And Service Date Acc To WeekType not matched";
                    customFailure(message,customAssert);
                }

                if(serviceDateStartingMonth >= 12){
                    serviceDateStartingMonth = 1;
                    serviceDateStartingYear = serviceDateStartingYear  + 1;
                }else {
                    serviceDateStartingMonth = serviceDateStartingMonth + 1;
                }
                if(serviceDateStartingMonth == 13) {
                    expectedServiceDate = DateUtils.getMonthEndDateInMMDDFormat(serviceDateStartingMonth -1, serviceDateStartingYear);
                }else {
                    expectedServiceDate = DateUtils.getMonthEndDateInMMDDFormat(serviceDateStartingMonth, serviceDateStartingYear);
                }

                serviceDateStartingMonth = Integer.parseInt(expectedServiceDate.split("/")[0]);
                serviceDateStartingDate = expectedServiceDate.split("/")[1];

                serviceDateStartingMonthString = DateUtils.getMonthinMMM(serviceDateStartingMonth);

                expectedServiceDate = serviceDateStartingDate + "-" + serviceDateStartingMonthString + "-" + serviceDateStartingYear;

                if(reportingDateStartingMonth == 12){
                    reportingDateStartingMonth = 1;
                    reportingDateStartingYear = reportingDateStartingYear  + 1;
                }else {
                    reportingDateStartingMonth = reportingDateStartingMonth + 1;
                }
                reportingDateStartingMonthString = DateUtils.getMonthinMMM(reportingDateStartingMonth);

                expectedReportingDate = reportingDateStartingDay + "-" + reportingDateStartingMonthString + "-" + reportingDateStartingYear;

            }

        }catch (Exception e){
            message = "Exception while validating frequency module test cases " + e.getStackTrace();
            customFailure(message,customAssert);
        }
        customAssert.assertAll();
    }

    private ArrayList<String> checkIfCSLCreatedOnServiceLevel(int serviceLevelId, CustomAssert customAssert) {

        logger.info("Checking if CSL created on service level");

        long timeSpent = 0;
        long cSLCreationTimeOut = 5000000L;
        long pollingTime = 5000L;
        ArrayList<String> childServiceLevelIds = new ArrayList<>();
        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray = new JSONArray();

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId,0,100);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < cSLCreationTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId,0,100);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Child Service Level created successfully ");

                        childServiceLevelIds = (ArrayList) ListDataHelper.getColumnIds(tabListResponse);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Child Service Level not created yet ");
                    }
                }
                if (childServiceLevelIds.size() == 0) {
//					customAssert.assertTrue(false, "Child Service level not created in " + cSLCreationTimeOut + " milli seconds for service level id " + serviceLevelId);
                }

            } else {
                customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId + " " + e.getMessage());
        }

        return childServiceLevelIds;
    }

    private int getActiveServiceLevelId(String message,String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

        int serviceLevelId = -1;
        try {

            serviceLevelId = getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);

            if (serviceLevelId != -1) {
                List<String> workFlowSteps;
                workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
                if (!performWorkFlowActions(message,slEntityTypeId, serviceLevelId, workFlowSteps, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    return -1;
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while getting an active an active service level id " + e.getMessage());
        }
        return serviceLevelId;
    }

    private Boolean performWorkFlowActions(String message,int entityTypeId, int entityId, List<String> workFlowSteps, CustomAssert customAssert) {

        Boolean workFlowStepActionStatus;

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {
            for (String workFlowStepToBePerformed : workFlowSteps) {

                workFlowStepActionStatus = workflowActionsHelper.performWorkFlowStepV2(entityTypeId,entityId,workFlowStepToBePerformed,customAssert);

                if (!workFlowStepActionStatus) {
                    message = "Unable to perform workflow action " + workFlowStepToBePerformed + " on service level id " + entityId;
                    customFailure(message,customAssert);
                    return false;
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getMessage());
            return false;

        }
        return true;
    }

    private int getServiceLevelId(String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

        int serviceLevelId = -1;

        CreateEntity createEntity = new CreateEntity(slCreationConfigFilePath, slCreationConfigFileName,
                extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest);

        String createPayload = createEntity.getCreatePayload("service levels", true, false);
        //Updating payload according to PCQ
        if (!JSONUtility.validjson(createPayload)) {
            throw new SkipException("Couldn't get Create Payload as valid Json for Flow [" + flowToTest + "] Thus Skipping the test");
        }
        JSONObject createPayloadJson = new JSONObject(createPayload);
        createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceDataCalculationQuery").put("values", DCQ);
        createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values", PCQ);
        createPayload = createPayloadJson.toString();

        String createResponse = null;

        if (createPayload != null) {
            logger.info("Hitting Create Api for Entity {}.", slEntity);
            Create createObj = new Create();
            createObj.hitCreate(slEntity, createPayload);
            createResponse = createObj.getCreateJsonStr();

            if (!ParseJsonResponse.validJsonResponse(createResponse)) {
                FileUtils.saveResponseInFile(slEntity + " Create API HTML.txt", createResponse);
            }
        }

        if (createResponse == null) {
            throw new SkipException("Couldn't get Create Response for Flow [" + flowToTest + "] Thus Skipping the test");
        }

        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

            logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                serviceLevelId = CreateEntity.getNewEntityId(createResponse, "service levels");

        } else {
            throw new SkipException("Couldn't get JSON Response for Create Flow [" + flowToTest + "] Thus Skipping the test");
        }
        return serviceLevelId;
    }

    //input date in format MMM-dd-yyyy
    private String getDayOfWeekForDate(String input_date,String dateFormat){

//        SimpleDateFormat format1=new SimpleDateFormat("MMM-dd-yyyy");
        SimpleDateFormat format1=new SimpleDateFormat(dateFormat);
        String finalDay = null;

        try {
            Date dt1 = format1.parse(input_date);
            DateFormat format2=new SimpleDateFormat("EEEE");
            finalDay=format2.format(dt1);

        }catch (Exception e){
            logger.error(e.getMessage());
        }

        return finalDay;
    }

    private Boolean checkReportingDateAndServiceDateAccToWeekType(String message,int cslId,int weekType,String expectedServiceDate,String expectedReportingDate, CustomAssert customAssert){

        Boolean validationStatus = true;
        try{

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId,cslId);
            String showResponse = show.getShowJsonStr();

            String serviceDate = ShowHelper.getValueOfField("duedate",showResponse).trim();
            String reportingDate = ShowHelper.getValueOfField("reportingdate",showResponse).trim();

            String weekDayServiceDate;
            String weekDayReportingDate;

            String dateFormat = DateUtils.getDateFormat(expectedServiceDate);
            weekDayServiceDate = getDayOfWeekForDate(expectedServiceDate,dateFormat);

            dateFormat = DateUtils.getDateFormat(expectedReportingDate);
            weekDayReportingDate = getDayOfWeekForDate(expectedReportingDate,dateFormat);

            String convertedServiceDate =  convertDate(expectedServiceDate,weekDayServiceDate,weekType);
            String convertedReportingDate =  convertDate(expectedReportingDate,weekDayReportingDate,weekType);

            String dateFormatConServiceDate = DateUtils.getDateFormat(convertedServiceDate);
            String dateFormatFromShowPage = DateUtils.getDateFormat(serviceDate);

            convertedServiceDate = DateUtils.converDateToAnyFormat(convertedServiceDate,dateFormatConServiceDate,dateFormatFromShowPage);

            if(!convertedServiceDate.equalsIgnoreCase(serviceDate)){
                message = convertedServiceDate+"Expected and Actual Service Date Didn't match"+serviceDate;
                customFailure(message,customAssert);
                validationStatus = false;
            }

            String dateFormatConReportingDate = DateUtils.getDateFormat(convertedReportingDate);
            String dateFormatReportingFromShowPage = DateUtils.getDateFormat(reportingDate);

            convertedReportingDate = DateUtils.converDateToAnyFormat(convertedReportingDate,dateFormatConReportingDate,dateFormatReportingFromShowPage);

            if(!convertedReportingDate.equalsIgnoreCase(reportingDate)){
                message = convertedReportingDate+ "Expected and Actual Reporting Date Didn't match"+reportingDate;
                customFailure(message,customAssert);
                validationStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while checking Reporting Date And Service Date According To Week Type ");
            customAssert.assertTrue(false,"Exception while checking Reporting Date And Service Date According To Week Type ");
            validationStatus = false;
        }

        return validationStatus;
    }

    private String convertDate(String date,String weekDayServiceDate,int weekType){

        String convertedDate = weekDayServiceDate;
        try {
            String[] dateArray = date.split("-");
            int day = Integer.parseInt(dateArray[0]);

            switch (weekDayServiceDate) {

                case "Saturday":

                    if (weekType == 5) {
                        day = day -1;
                    }
                    break;

                case "Sunday":

                    if (weekType == 5) {
                        day = day -2;
                    } else if (weekType == 6) {
                        day = day -1;
                    }
                    break;
            }

            if(day >=10){
                convertedDate = dateArray[1] + "-" + day + "-" + dateArray[2];
            }else {
                convertedDate = dateArray[1] + "-" + "0" + day + "-" + dateArray[2];
            }
        }catch (Exception e){
            logger.error("Exception while converting Date ");
        }
        return convertedDate;
    }

    private int getWeekType(String flowToTest){

        int weekType = -1;
        switch (flowToTest){

            case "sl with week type 5":
                weekType = 5;
                break;
            case "sl with week type 6":
                weekType = 6;
                break;
            case "sl with week type 7":
                weekType = 7;
                break;
        }

        return weekType;
    }

    @AfterClass
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
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

    private void customFailure(String message,CustomAssert customAssert){
        logger.error(message);
        customAssert.assertTrue(false,message);
    }

}
