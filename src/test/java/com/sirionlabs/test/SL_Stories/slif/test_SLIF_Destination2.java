package com.sirionlabs.test.SL_Stories.slif;

import com.sirionlabs.api.clientAdmin.dropDownType.DropDownTypeUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.Reports.ReportsDownloadHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.kafka.common.protocol.types.Field;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class test_SLIF_Destination2 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(test_SLIF_EndUser.class);

    @BeforeClass
    public void BeforeClass(){

        Check check = new Check();

        String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

        check.hitCheck(adminUserName,adminPassword);

    }

    @Test(enabled = true)
    public void TestDropDown(){

        DropDownTypeUpdate dropDownTypeUpdate = new DropDownTypeUpdate();

        CustomAssert customAssert = new CustomAssert();

        String supplierDropDownId = "2";
        String contractDropDownId = "1";
        String slaDropDownId = "12";

        int responseCode = dropDownTypeUpdate.hitDropdownAPI(20,"autocomplete",supplierDropDownId);

        if(responseCode != 302){
            customAssert.assertTrue(false,"Error while updating dropdown for supplier");
        }

        responseCode = dropDownTypeUpdate.hitDropdownAPI(20,"autocomplete",contractDropDownId);

        if(responseCode != 302){
            customAssert.assertTrue(false,"Error while updating dropdown for contract");
        }

        responseCode = dropDownTypeUpdate.hitDropdownAPI(20,"autocomplete",slaDropDownId);

        if(responseCode != 302){
            customAssert.assertTrue(false,"Error while updating dropdown for sla");
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void TestSupplierAutocomplete(){

        CustomAssert customAssert = new CustomAssert();

        String queryName = "ABC";
        String apiUrl = "/slintegration/supplier?query=" + queryName;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");


            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray supplierJsonArray = new JSONArray(responseBody);

                if (supplierJsonArray.length() == 0) {
                    customAssert.assertTrue(false, "Suppliers array size is equal to zero");
                }
                String nameFromAPIResponse;
                for(int i=0;i<supplierJsonArray.length();i++){

                    nameFromAPIResponse = supplierJsonArray.getJSONObject(i).get("name").toString();

                    if(!nameFromAPIResponse.contains(queryName)){
                        customAssert.assertTrue(false,nameFromAPIResponse + " is not as expected by querName " + queryName);
                    }
                }

            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void TestSLAAutocomplete(){

        CustomAssert customAssert = new CustomAssert();
        String queryName = "";
        String apiUrl = "/slintegration/SLA/" + 1124 + "?query=" + queryName;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray slJsonArray = new JSONArray(responseBody);

                if (slJsonArray.length() == 0) {
                    customAssert.assertTrue(false, "SL array size is equal to zero");
                }

                String nameFromAPIResponse;
                for(int i=0;i<slJsonArray.length();i++){

                    nameFromAPIResponse = slJsonArray.getJSONObject(i).get("name").toString();

                    if(!nameFromAPIResponse.contains(queryName)){
                        customAssert.assertTrue(false,nameFromAPIResponse + " is not as expected by querName " + queryName);
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void TestContractAutoComplete(){

        CustomAssert customAssert = new CustomAssert();

        String queryName = "testsug";

        String apiUrl = "/slintegration/contract/" + 1124 + "?query=" + queryName;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray contractJsonArray = new JSONArray(responseBody);

                if (contractJsonArray.length() == 0) {
                    customAssert.assertTrue(false, "Contract array size is equal to zero");
                }

                String nameFromAPIResponse;
                for(int i=0;i<contractJsonArray.length();i++){

                    nameFromAPIResponse = contractJsonArray.getJSONObject(i).get("name").toString();

                    if(!nameFromAPIResponse.contains(queryName)){
                        customAssert.assertTrue(false,nameFromAPIResponse + " is not as expected by querName " + queryName);
                    }
                }


            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void TestReportDownloadMonthly(){

        CustomAssert customAssert = new CustomAssert();
        try{

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();

            String slaIds = "1171,1072,1180";
            int entityTypeId = 346;
            int frequency = 2;
            int reportId = 514;
            String supplierId = "1050";

            String contractsId = "1110,1054";

            Map<String, String> formParam = getFormParamDownloadList(entityTypeId,frequency,supplierId,slaIds,contractsId);

            HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam,reportId);

            if (response.getStatusLine().toString().contains("200")) {

                String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
                String outputFileName = "ReportMonthly1.xlsx";

                Boolean downLoadStatus = dumpDownloadListWithDataResponseIntoFile(response,outputFilePath,outputFileName );

                if(!downLoadStatus){
                    customAssert.assertTrue(false,"Error while downloading Report");
                }

                XLSUtils xlsUtils = new XLSUtils(outputFilePath,outputFileName);

                String sheetName = "Data QA";

                List<String> rowData = XLSUtils.getExcelDataOfOneRow(outputFilePath,outputFileName,sheetName,4);

                String serviceDataColName = "SERVICE DATE";
                String columnName;
                int serviceDateColumnId = -1;
                for(int i = 0;i<rowData.size();i++){

                    columnName = rowData.get(i);

                    if(columnName.equals(serviceDataColName)){
                        serviceDateColumnId = i;
                        break;
                    }
                }

                if(serviceDateColumnId == -1){
                    customAssert.assertTrue(false,serviceDataColName + " not found int the excel ");
                    customAssert.assertAll();
                }

                int rowCount = xlsUtils.getRowCount(sheetName);
                String currentDate = DateUtils.getCurrentDateInAnyFormat("dd-MM-yyyy");

                String previousMonth = currentDate.split("-")[1];

                String serviceDateFormatExcel = "dd-MM-yyyy";
                String serviceDateExcel;
                String serviceDateMonth;
                List<String> expectedMonths;
                for(int i = 4;i<rowCount -2;i++ ){

                    serviceDateExcel = xlsUtils.getCellData(sheetName,serviceDateColumnId,i);

                    if(serviceDateFormatExcel.equals("dd-MM-yyyy")){


                        try{
                            serviceDateMonth = serviceDateExcel.split("-")[0];

                            expectedMonths = expectedPreviousMonths(Integer.parseInt(previousMonth));

                            if(!expectedMonths.contains(serviceDateMonth)){
                                customAssert.assertTrue(false,"Expected months doesn't contain the month service date month " + serviceDateMonth);
                            }

                        }catch (Exception e){
                            customAssert.assertTrue(false,"Exception while validating previous month date");
                        }

                    }

                }

            } else {
                logger.error("Error while downloading Report ");
                customAssert.assertTrue(false,"Error while downloading Report ");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


//One day Previous date lies in the service period of the csl
    @Test(enabled = true)
    public void TestReportDownloadDaily(){

        CustomAssert customAssert = new CustomAssert();

        List<String> cslIdListClientSeqIdFromExcelMonthlyFreq = new ArrayList<>();
        List<String> cslIdListClientSeqIdFromExcelQuarterlyFreq = new ArrayList<>();

        try{

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();

            int slaIds = 1180;
            int entityTypeId = 346;
            int frequency = 1;
            int reportId = 514;
            int supplierId = 1050;


            //It has to be have 2 contracts
            // logic is build in a way to cater csl of (1 monthly frequency and 1 quarterly frequency)
            List<Integer> contractsIdList = new ArrayList<>();

            String contractString = "1110,1054";
            contractsIdList.add(1110);
            contractsIdList.add(1054);

            Map<String, String> formParam = getFormParamDownloadList(entityTypeId,frequency,String.valueOf(supplierId),String.valueOf(slaIds), contractString);

            HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam,reportId);

            if (response.getStatusLine().toString().contains("200")) {

                String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
                String outputFileName = "ReportDaily.xlsx";

                Boolean downLoadStatus = dumpDownloadListWithDataResponseIntoFile(response,outputFilePath,outputFileName );

                if(!downLoadStatus){
                    customAssert.assertTrue(false,"Error while downloading Report");
                }

                XLSUtils xlsUtils = new XLSUtils(outputFilePath,outputFileName);

                String sheetName = "Data QA";

                List<String> rowData = XLSUtils.getExcelDataOfOneRow(outputFilePath,outputFileName,sheetName,4);

                String serviceDateColName = "SERVICE DATE";
                String frequencyColumnName = "FREQUENCY";

                String columnName;
                int serviceDateColumnId = -1;

                int compFrequencyColumnId = -1;

                for(int i = 0;i<rowData.size();i++){

                    columnName = rowData.get(i);

                    if(columnName.equals(serviceDateColName)){
                        serviceDateColumnId = i;
                    }
                    if(columnName.equals(frequencyColumnName)){
                        compFrequencyColumnId = i;
                    }
                }

                if(serviceDateColumnId == -1){
                    customAssert.assertTrue(false,serviceDateColName + " not found int the excel ");
                    customAssert.assertAll();
                }

                if(compFrequencyColumnId == -1){
                    customAssert.assertTrue(false,compFrequencyColumnId + " not found int the excel ");
                    customAssert.assertAll();
                }

                int rowCount = xlsUtils.getRowCount(sheetName);

                String previousDate = DateUtils.getPreviousDateInMM_DD_YYYY(DateUtils.getCurrentDateInAnyFormat("MM-dd-YYYY"));

                String serviceDateFormatExcel = "dd-MM-yyyy";
                String serviceDateExcel;
                String computationFrequency;
                String serviceDateMonth;
                String serviceDateYear;

                String startDate;
                String endDate;
                String cslId;

                for(int i = 4;i<rowCount -2;i++ ){

                    serviceDateExcel = xlsUtils.getCellData(sheetName,serviceDateColumnId,i);
                    computationFrequency = xlsUtils.getCellData(sheetName,compFrequencyColumnId,i);

                    cslId = xlsUtils.getCellData(sheetName,0,i);
                    cslId = cslId.split("/")[0];

                    if(serviceDateFormatExcel.equals("dd-MM-yyyy")){

                        try{

                            if(computationFrequency.equals("Monthly")){

                                serviceDateMonth =  serviceDateExcel.split("-")[0];
                                serviceDateYear =  "20" + serviceDateExcel.split("-")[2];

                                startDate = DateUtils.getMonthStartDateInMM_DDFormat(Integer.parseInt(serviceDateMonth));
                                startDate = startDate + "-" + serviceDateYear;
                                endDate = DateUtils.getMonthEndDateInMM_DDFormat(Integer.parseInt(serviceDateMonth),Integer.parseInt(serviceDateYear));
                                endDate = endDate + "-" + serviceDateYear;

                                Boolean dateInRange = DateUtils.isDateWithinRange(previousDate,startDate,endDate,"MM-dd-yyyy");

                                if(!dateInRange){
                                    customAssert.assertTrue(false,"Previous Date does not lie in the range of expected start date and end date of service period");
                                }
                                cslIdListClientSeqIdFromExcelMonthlyFreq.add(cslId);

                            }else if(computationFrequency.equals("Quarterly")){

                                serviceDateMonth =  serviceDateExcel.split("-")[0];
                                serviceDateYear =  "20" + serviceDateExcel.split("-")[2];


                                startDate = DateUtils.getQuarterStartDateInMM_DDFormat(Integer.parseInt(serviceDateMonth));
                                startDate = startDate + "-" + serviceDateYear;
                                endDate = DateUtils.getQuarterEndDateInMM_DDFormat(Integer.parseInt(serviceDateMonth));
                                endDate = endDate + "-" + serviceDateYear;

                                Boolean dateInRange = DateUtils.isDateWithinRange(previousDate,startDate,endDate,"MM-dd-yyyy");

                                if(!dateInRange){
                                    customAssert.assertTrue(false,"Previous Date does not lie in the range of expected start date and end date of service period");
                                }
                                cslIdListClientSeqIdFromExcelQuarterlyFreq.add(cslId);
                            }

                        }catch (Exception e){
                            customAssert.assertTrue(false,"Exception while validating previous month date");
                        }

                    }

                }
                String currentDate = DateUtils.getCurrentDateInAnyFormat("dd-MM-yyyy");

                int startDateMonth = Integer.parseInt(currentDate.split("-")[1]);
                int year = Integer.parseInt(currentDate.split("-")[2]);

                String startDateMonthly = DateUtils.getMonthStartDateInMM_DDFormat(startDateMonth);
                String endDateMonthly = DateUtils.getMonthEndDateInMMDDFormat(startDateMonth,year);
                endDateMonthly = endDateMonthly.replace("/","-");

                startDateMonthly = startDateMonthly + "-" + year;
                endDateMonthly = endDateMonthly + "-" + year;
                int monthlyId = 4;

                try {
                    Check check = new Check();
                    check.hitCheck();

                    List<String> monthlyCslListingPage = getListingData(supplierId, contractsIdList, startDateMonthly, endDateMonthly, slaIds, monthlyId);

                    for (String monthCsl : monthlyCslListingPage) {
                        if(!cslIdListClientSeqIdFromExcelMonthlyFreq.contains(monthCsl)){
                            logger.error("CSL ID From monthly Listing page list not found in excel " + monthCsl);
                            customAssert.assertTrue(false,"CSL ID From monthly Listing page list not found in excel " + monthCsl);
                        }
                    }
                }catch (Exception e){
                    logger.error("Exception while validating monthly CSL from from Listing page");
                    customAssert.assertTrue(false,"Exception while validating monthly CSL from from Listing page");
                }
                try {

                    currentDate = DateUtils.getCurrentDateInAnyFormat("dd-MM-yyyy");

                    startDateMonth = Integer.parseInt(currentDate.split("-")[1]);
                    year = Integer.parseInt(currentDate.split("-")[2]);

                    String startDateQuarterly = DateUtils.getQuarterStartDateInMM_DDFormat(startDateMonth);
                    String endDateQuarterly = DateUtils.getQuarterEndDateInMM_DDFormat(startDateMonth);
                    endDateQuarterly = endDateQuarterly.replace("/","-");

                    startDateQuarterly = startDateQuarterly + "-" + year;
                    endDateQuarterly = endDateQuarterly + "-" + year;

                    int quarterlyId = 6;
                    List<String> quarterlyCslListingPage = getListingData(supplierId, contractsIdList, startDateQuarterly, endDateQuarterly, slaIds, quarterlyId);

                    for (String quarterCsl : quarterlyCslListingPage) {
                        if(!cslIdListClientSeqIdFromExcelQuarterlyFreq.contains(quarterCsl)){
                            logger.error("CSL ID From quarterly Listing page list not found in excel " + quarterCsl);
                            customAssert.assertTrue(false,"CSL ID From quarterly Listing page list not found in excel " + quarterCsl);
                        }
                    }

                }catch (Exception e){
                    logger.error("Exception while validating quarter CSL from from Listing page");
                    customAssert.assertTrue(false,"Exception while validating quarter CSL from from Listing page");
                }


            } else {
                logger.error("Error while downloading Report ");
                customAssert.assertTrue(false,"Error while downloading Report ");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }finally {
            Check check = new Check();

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

            check.hitCheck(adminUserName,adminPassword);
        }

        customAssert.assertAll();
    }


    @Test(enabled = true)
    public void TestReportDownloadQuarterly(){

        CustomAssert customAssert = new CustomAssert();
        try{

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();

            String slaIds = "1171,1072,1180";
            int entityTypeId = 346;
            int frequency = 3;
            int reportId = 514;
            String supplierId = "1050";

            String contractsId = "1110,1054";

            Map<String, String> formParam = getFormParamDownloadList(entityTypeId,frequency,supplierId,slaIds,contractsId);

            HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam,reportId);

            if (response.getStatusLine().toString().contains("200")) {

                String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
                String outputFileName = "ReportQuarterly.xlsx";

                Boolean downLoadStatus = dumpDownloadListWithDataResponseIntoFile(response,outputFilePath,outputFileName );

                if(!downLoadStatus){
                    customAssert.assertTrue(false,"Error while downloading Report");
                }

                XLSUtils xlsUtils = new XLSUtils(outputFilePath,outputFileName);

                String sheetName = "Data QA";

                List<String> rowData = XLSUtils.getExcelDataOfOneRow(outputFilePath,outputFileName,sheetName,4);

                String serviceDataColName = "SERVICE DATE";
                String columnName;
                int serviceDateColumnId = -1;
                for(int i = 0;i<rowData.size();i++){

                    columnName = rowData.get(i);

                    if(columnName.equals(serviceDataColName)){
                        serviceDateColumnId = i;
                        break;
                    }
                }

                if(serviceDateColumnId == -1){
                    customAssert.assertTrue(false,serviceDataColName + " not found int the excel ");
                    customAssert.assertAll();
                }

                int rowCount = xlsUtils.getRowCount(sheetName);
                String currentDate = DateUtils.getCurrentDateInAnyFormat("dd-MM-yyyy");

                String previousMonth = currentDate.split("-")[1];

                String serviceDateFormatExcel = "dd-MM-yyyy";
                String serviceDateExcel;
                String serviceDateMonth;
                List<String> expectedMonths;
                for(int i = 4;i<rowCount -2;i++ ){

                    serviceDateExcel = xlsUtils.getCellData(sheetName,serviceDateColumnId,i);

                    if(serviceDateFormatExcel.equals("dd-MM-yyyy")){


                        try{
                            serviceDateMonth = serviceDateExcel.split("-")[0];

                            expectedMonths = expectedPreviousQuarterMonths(Integer.parseInt(previousMonth));

                            if(!expectedMonths.contains(serviceDateMonth)){
                                customAssert.assertTrue(false,"Expected months doesn't contain the month service date month " + serviceDateMonth);
                            }

                        }catch (Exception e){
                            customAssert.assertTrue(false,"Exception while validating previous month date");
                        }

                    }

                }

            } else {
                logger.error("Error while downloading Report ");
                customAssert.assertTrue(false,"Error while downloading Report ");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    @Test(enabled = true)
    public void TestComputationFrequency(){

        CustomAssert customAssert = new CustomAssert();

        try {

            String apiUrl = "/slintegration/computationfrequency";

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(apiUrl, headers).getResponse();

            if (response.getResponseCode() != 200) {
                logger.error("API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            List<String> expectedComputationFrequencyList = Arrays.asList("Annuallyyyy","Bi-Monthly","Bi-Weekly","Daily frequency","Half-Yearly","Half-Yearly Periodic first nth working day","Half-Yearly Periodic last nth working day","Monthly (Date)","Monthly (Day)","Monthly first nth working day","Monthly last nth working day","One-Time","Periodic -Date in every 3 period","Periodic -Day in every 2 period","Periodic first nth working day","Periodic Half Yearly - Last Wednesday","Periodic last day","Periodic last nth working day","Periodic Last Working Day","Quarterly","Quarterly (Periodic) First nth working day","Quarterly (Periodic) last nth working day","Weekly","Yearly");
            List<String> actualComputationFrequencyList = new ArrayList<>();

            if (!APIUtils.validJsonResponse(responseBody)) {
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false, "API Response is not a valid Json");
            } else {
                JSONArray responseBodyJsonArray = new JSONArray(responseBody);

                for(int i =0;i<responseBodyJsonArray.length();i++){

                    actualComputationFrequencyList.add(responseBodyJsonArray.getJSONObject(i).get("name").toString());
                }

                for(String expectedComputationFrequency : expectedComputationFrequencyList){
                    if(!actualComputationFrequencyList.contains(expectedComputationFrequency)){
                        customAssert.assertTrue(false,expectedComputationFrequency + " not found in Actual Frequency List");
                    }
                }
            }

        }catch (Exception e) {
                logger.error("Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestDestinationDownload(){

        CustomAssert customAssert = new CustomAssert();

        try{

            //Can be any other value for destination also
            int destinationId = 25;

            String apiUrl = "/slintegration/destination/download/" + destinationId;

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            HttpResponse response = executor.GetHttpResponseForGetAPI(apiUrl, headers);

            if (response.getStatusLine().toString().contains("200")) {

                String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
                String outputFileName = "Destination.xlsx";

                Boolean downLoadStatus = dumpDownloadListWithDataResponseIntoFile(response,outputFilePath,outputFileName );

                if(!downLoadStatus){
                    customAssert.assertTrue(false,"Error while downloading Report");
                }else {

                    XLSUtils xlsUtils = new XLSUtils(outputFilePath, outputFileName);
                    String sheetName = "Data QA";
                    String rawDataString = xlsUtils.getCellData(sheetName,0,0);
                    String firstRowFirstColumnData = "Raw Data";

                    if(!rawDataString.equals(firstRowFirstColumnData)){
                        customAssert.assertTrue(false,"Expect first row first column is not expected string " + firstRowFirstColumnData);
                    }
                }


            } else {
                logger.error("Error while downloading Report ");
                customAssert.assertTrue(false,"Error while downloading Report ");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @AfterClass
    public void afterClass(){
        Check check = new Check();
        check.hitCheck();
    }

    private Map<String, String> getFormParamDownloadList(Integer entityTypeId,int frequency,String supplierId,String slaId,String contractId) {

        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;
        String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");

        int offset = 0;
        int size = 20;
        String orderByColumnName = "id";
        String orderDirection = "desc nulls last";


        jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + "," +
                "\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"" + orderDirection + "\":" +
                "\"desc nulls last\",\"frequency\":" + frequency + "," +
                "\"supplierId\":" + supplierId + "," +
                "\"slaIds\":[" + slaId + "],\"" +
                "contractIds\":[" + contractId + "],\"" +
                "filterJson\":{}}}";

        jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + "," +
                "\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"" + orderDirection + "\":" +
                "\"desc nulls last\",\"frequency\":" + frequency + "," +
                "\"supplierId\":" + supplierId + "," +
                "\"slaIds\":[],\"" +
                "contractIds\":[" + contractId + "],\"" +
                "filterJson\":{}}}";

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String outputFileName) {

        Boolean status;
        String outputFile;
        FileUtils fileUtil = new FileUtils();

        try {
            outputFile = outputFilePath + "/" + outputFileName;
            status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status) {
                logger.info("DownloadListWithData file generated at {}", outputFile);
                status = true;
            }
        }catch (Exception e){
            status = false;
        }
        return status;
    }

    private List<String> expectedPreviousQuarterMonths(int month){

        List<String> previousMonths = new ArrayList<>();

        if(month == 1 || month == 2 || month == 3){
            previousMonths = Arrays.asList("10","11","12");

        }else if(month == 4 || month == 5 || month == 6){
            previousMonths = Arrays.asList("1","2","3");
        }else if(month == 7 || month == 8 || month == 9){
            previousMonths = Arrays.asList("4","5","6");
        }else if(month == 10 || month == 11 || month == 12) {
            previousMonths = Arrays.asList("7", "8", "9");
        }
        return previousMonths;

    }

    private List<String> expectedPreviousMonths(int month){

        List<String> previousMonths = new ArrayList<>();

        if(month == 1){
            previousMonths = Arrays.asList("12");

        } else if(month > 1 && month < 13){
            String previousMonth = String.valueOf(month -1);
            previousMonths = Arrays.asList(previousMonth);
        }

        return previousMonths;

    }

    private List<String> getListingData(int supplierId,List<Integer> contractId,String startDate,String endDate,int slaId,int frequency){

        String payload = null;
        if(frequency == 6){
            payload = "{\"filterMap\":{\"entityTypeId\":15,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplierId + "\",\"name\":\"Berkshire Hathaway - test 1\"}]}," +
                    "\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId.get(0) + "\",\"name\":\"Independent Contractor Agreement - Berkshire Hathaway\"}]}," +
                    "\"filterId\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"24\":{\"filterId\":\"24\",\"filterName\":\"dueDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null," +
                    "\"start\":\"" + startDate + "\",\"end\":\"" + endDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"29\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1006\",\"name\":\"Quarterly\"}," +
                    "{\"id\":\"1003\",\"name\":\"Monthly (Date)\"},{\"id\":\"1004\",\"name\":\"Monthly (Day)\"}," +
                    "{\"id\":\"1013\",\"name\":\"Monthly first nth working day\"}," +
                    "{\"id\":\"1012\",\"name\":\"Monthly last nth working day\"}]}," +
                    "\"filterId\":29,\"filterName\":\"frequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"124\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + slaId + "\",\"name\":\"Abandoned ; Call Rate (Europe)\"}]}," +
                    "\"filterId\":124,\"filterName\":\"slItem\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"400\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + frequency + "\",\"name\":\"Quarterly\"}]}," +
                    "\"filterId\":400,\"filterName\":\"datafrequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":12142,\"columnQueryName\":\"id\"}]}";
        }else {
            payload = "{\"filterMap\":{\"entityTypeId\":15,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplierId + "\",\"name\":\"Berkshire Hathaway - test 1\"}]}," +
                    "\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId.get(0) + "\",\"name\":\"Independent Contractor Agreement - Berkshire Hathaway\"}," +
                    "{\"id\":\"" + contractId.get(1) + "\",\"name\":\"Sirion Performance Contracts\"}]},\"filterId" +
                    "\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"24\":" +
                    "{\"filterId\":\"24\",\"filterName\":\"dueDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                    "\"dayOffset\":null,\"duration\":null,\"start\":\"" + startDate + "\",\"end\":\"" + endDate + "\"," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"124\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + slaId + "\",\"name\":\"Abandoned ; Call Rate (Europe)\"}]}," +
                    "\"filterId\":124,\"filterName\":\"slItem\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"400\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + frequency + "\",\"name\":\"Monthly\"}]}," +
                    "\"filterId\":400,\"filterName\":\"datafrequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                    "\"selectedColumns\":[{\"columnId\":12148,\"columnQueryName\":\"bulkcheckbox\"}," +
                    "{\"columnId\":12142,\"columnQueryName\":\"id\"}]}";
        }
        ListRendererListData listRendererListData = new ListRendererListData();
        listRendererListData.hitListRendererListData(265,payload);
        String listingResponse = listRendererListData.getListDataJsonStr();

        JSONObject listingResponseJson = new JSONObject(listingResponse);

        JSONArray dataArray = listingResponseJson.getJSONArray("data");
        JSONObject indData;
        JSONArray indDataArray;
        String columnName;
        String columnValue;

        List<String> cslIdList = new ArrayList<>();
        for(int i =0;i < dataArray.length();i++ ){

            indData = dataArray.getJSONObject(i);

            indDataArray = JSONUtility.convertJsonOnjectToJsonArray(indData);

            innerLoop:
            for(int j=0;j<indDataArray.length();j++){

                columnName = indDataArray.getJSONObject(j).get("columnName").toString();

                if(columnName.equals("id")){
                    columnValue = indDataArray.getJSONObject(j).get("value").toString().split(":;")[0];
                    cslIdList.add("CSL" + columnValue);
                    break innerLoop;
                }

            }
        }

        return cslIdList;
    }

}
