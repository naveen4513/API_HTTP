package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class test_SIR215263 {

    private String configFilePath;
    private String configFileName;

    private String slConfigFilePath;
    private String slConfigFileName;

    private Integer slEntityTypeId;
    private Integer cslEntityTypeId;

    private String auditLogUser;
    private String adminUser;
    private String uploadFilePath;

    private int slMetaDataUploadTemplateId;
    private int uploadIdSL_PerformanceDataTab;

    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    private final static Logger logger = LoggerFactory.getLogger(test_SIR215263.class);

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("testSIR215263ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("testSIR215263ConfigFileName");

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");

        auditLogUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");
        adminUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "adminuser");

        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slmetadatauploadtemplateid"));
        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadidslperformancecdatatab"));
        uploadFilePath = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadfilepath");
    }

    //C63504	 	C63505	 C63506
    @Test
    public void TestMaxPeriodDaysFunction(){

        CustomAssert customAssert = new CustomAssert();

        try {

            List<String> workFlowSteps;
            ArrayList<String> childServiceLevelIdList;

            String flowToTest = "sl automation flow";
            int serviceLevelId;

            String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
            //String DCQ = "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}},\"script_fields\":{\"Met/Missed\":{\"script\":\"if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 100000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
            String DCQ = "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}},\"script_fields\":{\"Met/Missed\":{\"script\":\"if(params['_source']['Max Time Monthly1'] != ''){if(doc['Max Time Monthly1'].value < 100000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);
//            serviceLevelId = 8051;

            slToDelete.add(serviceLevelId);

            if (serviceLevelId != -1) {
                workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active

                if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, auditLogUser, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    customAssert.assertAll();
                }

                childServiceLevelIdList = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

                addCSLToDelete(childServiceLevelIdList);

                if(childServiceLevelIdList.size() == 0){
                    customAssert.assertTrue(false,"Child Service Level not created for SL " + serviceLevelId);
                    customAssert.assertAll();
                }
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedatuploadsuccessmsg");
                String rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilesuccessmsg");
                String uploadFilePath;
                String performanceDataFormatFileName;

                String cslRawDataFileName;
                int childServiceLevelId;

                uploadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadfilepath");
                performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
                cslRawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

                String performanceDataFormatFileNameToUpload;
                String rawDataFileNameToUpload;

                for(int fileNumber = 1;fileNumber <= 6; fileNumber++) {

                    performanceDataFormatFileNameToUpload = performanceDataFormatFileName + fileNumber + ".xlsm";
                    rawDataFileNameToUpload = cslRawDataFileName + fileNumber + ".xlsx";

                    if (!serviceLevelHelper.uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileNameToUpload, expectedMsg, customAssert)) {
                        customAssert.assertTrue(false, "Error while performance data file upload");
                        customAssert.assertAll();
                    }

                    if (!serviceLevelHelper.validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileNameToUpload, customAssert)) {

                        customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
                    }

                    childServiceLevelId = Integer.parseInt(childServiceLevelIdList.get(fileNumber));
                    rawDataFileValidMsg = "200:;";
                    if(!serviceLevelHelper.uploadRawDataCSL(childServiceLevelId,uploadFilePath,rawDataFileNameToUpload,rawDataFileValidMsg,customAssert)){
                        customAssert.assertTrue(false,"Raw Data File Uploaded successfully for CSL Number " + fileNumber);
                    }
                    Thread.sleep(5000);
                    if(!serviceLevelHelper.validateStructuredPerformanceDataCSL(childServiceLevelId,rawDataFileNameToUpload,"Done","View Structured Data",auditLogUser,customAssert)){

                        customAssert.assertTrue(false,"Raw Data File Uploaded Unsuccessfully for CSL " + fileNumber);
                    }

                    List<String> dateUploaded = XLSUtils.getOneColumnDataFromMultipleRows(uploadFilePath,rawDataFileNameToUpload,"Raw Data",2,1,12);

                    ArrayList<String> monthList = new ArrayList<>();
                    ArrayList<String> yearList = new ArrayList<>();

                    String dateFormatUploaded = XLSUtils.getOneCellValue(uploadFilePath,performanceDataFormatFileNameToUpload,"Format Sheet",4,2);

                    getMonthListYearList(dateUploaded,dateFormatUploaded,monthList,yearList,customAssert);

                    ArrayList<String> maxPeriodMonthlyList = getMaxPeriodMonthly(monthList,yearList);

                    ArrayList<String> maxPeriodQuarterlyList = getMaxPeriodQuarterly(monthList,yearList);

                    ArrayList<String> maxPeriodSemiAnnuallyList = getMaxPeriodSemiAnnually(monthList,yearList);


                    if(!validateMaxPeriodCalculatedValues(childServiceLevelId,
                            maxPeriodMonthlyList,maxPeriodQuarterlyList,maxPeriodSemiAnnuallyList,customAssert)){
                        customAssert.assertTrue(false,"Max Period Days Validated unsuccessfully for Date Format " + dateFormatUploaded);

                    }else {
                        logger.info("Max Period Days Validated Successfully for Date Format " + dateFormatUploaded);
                    }
//
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating max period days function " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating max period days function " + e.getMessage());

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

    private void getMonthListYearList(List<String> dateUploadedList,String dateFormat,ArrayList<String> monthList,ArrayList<String> yearList,CustomAssert customAssert){

        String month = "";
        String year = "";
        try {
            for(String date : dateUploadedList) {

                if (dateFormat.equalsIgnoreCase("MM-dd-yyyy")) {
                    month = date.split("-")[0];
                    year = date.split("-")[2];

                } else if (dateFormat.equalsIgnoreCase("yyyy/MM/dd")) {
                    month = date.split("/")[1];
                    year = date.split("/")[0];

                } else if (dateFormat.equalsIgnoreCase("dd/MM/yyyy")) {
                    month = date.split("/")[1];
                    year = date.split("/")[2];

                } else if (dateFormat.equalsIgnoreCase("MM/dd/yyyy")) {
                    month = date.split("/")[0];
                    year = date.split("/")[2];

                } else if (dateFormat.equalsIgnoreCase("yyyy-MM-dd")) {
                    month = date.split("-")[1];
                    year = date.split("-")[0];

                } else if (dateFormat.equalsIgnoreCase("MM-dd-yyyy")) {
                    month = date.split("-")[0];
                    year = date.split("-")[2];

                } else if (dateFormat.equalsIgnoreCase("dd-MM-yyyy")) {
                    month = date.split("-")[1];
                    year = date.split("-")[2];
                }else if(dateFormat.equalsIgnoreCase("ddMMyyyy")) {
                    month = date.substring(2,4);
                    year = date.substring(4,8);
                } else if (dateFormat.equalsIgnoreCase("yyyyMMdd")) {
                    month = date.substring(4, 6);
                    year = date.substring(0, 4);
                }else if (dateFormat.equalsIgnoreCase("MMddyyyy")) {

                    month = date.substring(0,2);
                    year = date.substring(4,8);
                }


                monthList.add(month);
                yearList.add(year);
            }
        }catch (Exception e){
            logger.error("Exception while validating Max Period Days Function On View Structured Data");
            customAssert.assertTrue(false,"Exception while validating Max Period Days Function On View Structured Data");
        }

    }

    private ArrayList<String> getMaxPeriodMonthly(ArrayList<String> monthList,ArrayList<String> yearList){

        ArrayList<String> MaxPeriodMonthlyList = new ArrayList<>();
        String month;
        String maxPeriodMonthly = "";
        try {
            for (int i = 0; i < monthList.size(); i++) {

                month = monthList.get(i);

                if (month.equalsIgnoreCase("02") && ((Integer.parseInt(yearList.get(i)) % 4) == 0)) {
                    maxPeriodMonthly = "29";
                } else if (month.equalsIgnoreCase("02") && ((Integer.parseInt(yearList.get(i)) % 4) != 0)) {
                    maxPeriodMonthly = "28";
                } else if (month.equalsIgnoreCase("01") || month.equalsIgnoreCase("03") || month.equalsIgnoreCase("05") || month.equalsIgnoreCase("07") || month.equalsIgnoreCase("08") || month.equalsIgnoreCase("10") || month.equalsIgnoreCase("12")) {
                    maxPeriodMonthly = "31";
                } else if (month.equalsIgnoreCase("04") || month.equalsIgnoreCase("06") || month.equalsIgnoreCase("09") || month.equalsIgnoreCase("11")) {
                    maxPeriodMonthly = "30";
                }
                MaxPeriodMonthlyList.add(maxPeriodMonthly);
            }
        }catch (Exception e){
            logger.error("Exception while getting Max Period Monthly List");
        }
        return MaxPeriodMonthlyList;
    }

    private ArrayList<String> getMaxPeriodQuarterly(ArrayList<String> monthList,ArrayList<String> yearList){

        ArrayList<String> MaxPeriodQuarterlyList = new ArrayList<>();
        String month;
        String maxPeriodQuarterly = "";
        try {
            for (int i = 0; i < monthList.size(); i++) {

                month = monthList.get(i);

                if ((month.equalsIgnoreCase("01") || month.equalsIgnoreCase("02") || month.equalsIgnoreCase("03")) && ((Integer.parseInt(yearList.get(i)) % 4) == 0)) {
                    maxPeriodQuarterly = "91";
                } else if ((month.equalsIgnoreCase("01") || month.equalsIgnoreCase("02") || month.equalsIgnoreCase("03")) && ((Integer.parseInt(yearList.get(i)) % 4) != 0)) {
                    maxPeriodQuarterly = "90";
                } else if (month.equalsIgnoreCase("04") || month.equalsIgnoreCase("05") || month.equalsIgnoreCase("06")) {
                    maxPeriodQuarterly = "91";
                } else if (month.equalsIgnoreCase("07") || month.equalsIgnoreCase("08") || month.equalsIgnoreCase("09")) {
                    maxPeriodQuarterly = "92";
                } else if (month.equalsIgnoreCase("10") || month.equalsIgnoreCase("11") || month.equalsIgnoreCase("12")) {
                    maxPeriodQuarterly = "92";
                }
                MaxPeriodQuarterlyList.add(maxPeriodQuarterly);
            }
        }catch (Exception e){
            logger.error("Exception while getting Max Period Quarterly List");
        }
        return MaxPeriodQuarterlyList;
    }

    private ArrayList<String> getMaxPeriodSemiAnnually(ArrayList<String> monthList,ArrayList<String> yearList){

        ArrayList<String> MaxPeriodSemiAnnuallyList = new ArrayList<>();
        String month;
        int monthValue;
        String maxPeriodSemiAnnually = "";
        try {
            for (int i = 0; i < monthList.size(); i++) {

                month = monthList.get(i);

                try{
                    monthValue = Integer.parseInt(month);

                }catch (NumberFormatException nfe){

                    monthValue = Integer.parseInt(DateUtils.getMonthindigit(month));

                }
                if (monthValue > 6) {
                    maxPeriodSemiAnnually = "184";
                } else {
                    if ((Integer.parseInt(yearList.get(i)) % 4) == 0) {
                        maxPeriodSemiAnnually = "182";
                    } else {
                        maxPeriodSemiAnnually = "181";
                    }

                }
                MaxPeriodSemiAnnuallyList.add(maxPeriodSemiAnnually);
            }
        }catch (Exception e){
            logger.error("Exception while getting Max Period Semi Annually List");
        }
        return MaxPeriodSemiAnnuallyList;
    }

    private Boolean validateMaxPeriodCalculatedValues(int childSlId,
                                                      ArrayList maxPeriodMonthlyList,
                                                      ArrayList maxPeriodQuarterlyList,
                                                      ArrayList maxPeriodSemiAnnuallyList,
                                                      CustomAssert customAssert){


        Boolean validationStatus = true;

        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        try{

            String structurePerformanceDataResponse = serviceLevelHelper.getStructuredPerformanceData(childSlId);
            String documentId = serviceLevelHelper.getDocumentIdFromCSLRawDataTab(structurePerformanceDataResponse);

            String payload = "{\"documentId\":" + documentId + ",\"offset\":0,\"size\":20,\"childSlaId\":" + childSlId +"}";

            SLDetails slDetails = new SLDetails();
            slDetails.hitSLDetailsList(payload);
            String slDetailsResponse = slDetails.getSLDetailsResponseStr();

            String maxPeriodMonthly = "";
            String maxPeriodQuarterly = "";
            String maxPeriodSemiAnnually = "";

            ArrayList<String> maxPeriodMonthlyListActual = new ArrayList<>() ;
            ArrayList<String> maxPeriodQuarterlyListActual= new ArrayList<>() ;
            ArrayList<String> maxPeriodSemiAnnuallyListActual= new ArrayList<>() ;

            if(APIUtils.validJsonResponse(slDetailsResponse)){

                JSONObject slDetailsResponseJson = new JSONObject(slDetailsResponse);
                JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");
                JSONObject indDataRecord;
                JSONArray indRowDataRecordArray;
                JSONObject indRowColumnRecord;
                String columnName;
                for(int dataArrayRecordNo = 0;dataArrayRecordNo<dataArray.length();dataArrayRecordNo++){

                    indDataRecord = dataArray.getJSONObject(dataArrayRecordNo);

                    indRowDataRecordArray = JSONUtility.convertJsonOnjectToJsonArray(indDataRecord);

                    for(int columnNo = 0;columnNo<indRowDataRecordArray.length();columnNo++){

                        indRowColumnRecord = indRowDataRecordArray.getJSONObject(columnNo);

                        columnName = indRowColumnRecord.get("columnName").toString();
                        switch (columnName) {
                            case "Max Time Monthly1":
                                maxPeriodMonthly = indRowColumnRecord.get("columnValue").toString();
                                break;
                            case "Max Time Quarter1":
                                maxPeriodQuarterly = indRowColumnRecord.get("columnValue").toString();
                                break;
                            case "Max Time Semi Annual1":
                                maxPeriodSemiAnnually = indRowColumnRecord.get("columnValue").toString();
                                break;
                        }

                    }
                    maxPeriodMonthlyListActual.add(maxPeriodMonthly);
                    maxPeriodQuarterlyListActual.add(maxPeriodQuarterly);
                    maxPeriodSemiAnnuallyListActual.add(maxPeriodSemiAnnually);
                }
                int totalNumberOfRowsToValidate = maxPeriodMonthlyList.size();

                for(int i =0;i<totalNumberOfRowsToValidate;i++){

                    if(!maxPeriodMonthlyList.get(i).equals(maxPeriodMonthlyListActual.get(i))){
                        logger.error("Expected and Actual Value for Max Period Days monthly didn't match for record " + i + " and CSL ID " + childSlId);
                        customAssert.assertTrue(false,"Expected and Actual Value for Max Period Days monthly didn't match for record " + i + " and CSL ID " + childSlId);
                        validationStatus = false;
                    }

                    if(!maxPeriodQuarterlyList.get(i).equals(maxPeriodQuarterlyListActual.get(i))){
                        logger.error("Expected and Actual Value for Max Period Days Quarterly didn't match for record " + i + " and CSL ID " + childSlId);
                        customAssert.assertTrue(false,"Expected and Actual Value for Max Period Days Quarterly didn't match for record " + i + " and CSL ID " + childSlId);
                        validationStatus = false;
                    }

                    if(!maxPeriodSemiAnnuallyList.get(i).equals(maxPeriodSemiAnnuallyListActual.get(i))){
                        logger.error("Expected and Actual Value for Max Period Days Semi Annual didn't match for record " + i + " and CSL ID " + childSlId);
                        customAssert.assertTrue(false,"Expected and Actual Value for Max Period Days Semi Annual didn't match for record " + i + " and CSL ID " + childSlId);
                        validationStatus = false;
                    }
                }

            }else {
                logger.error("View Structured is not valid Json");
                customAssert.assertTrue(false,"View Structured is not valid Json");
                validationStatus = false;
            }


        }catch (Exception e){
            logger.error("Exception while validating Max Period Calculated Values " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Max Period Calculated Values " + e.getMessage());
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
            logger.error("Error while adding child service level to deleted list " + e.getMessage());
        }
    }

}
