package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.Reports.ReportsDownloadHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSLReports {

    private final static Logger logger = LoggerFactory.getLogger(TestSLReports.class);

    private String configFilePath;
    private String configFileName;

    private int slEntityTypeId = 14;
    private int cslEntityTypeId = 15;

    private String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLReports";
    String cslEntity = "child service levels";
    String slEntity = "service levels";
    String dbHostName;
    String dbPortName;
    String dbName;
    String dbUserName;
    String dbPassowrd;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLReportsConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLReportsConfigFileName");

        dbHostName = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassowrd = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

    }

    @DataProvider(name = "cslReportsToValidate", parallel = false)
    public Object[][] cslReportsToValidate() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] reportIdsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"csl reports to validate for listing").split(",");

        for (String flowToTest : reportIdsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "cslReportsToValidate",enabled = true)
    public void TestReportListingCSLReports(String listId){

        CustomAssert customAssert = new CustomAssert();

        try{

            ReportRendererListData reportRendererListData = new ReportRendererListData();

            String genericFilterId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","filter id");
            String customCurrencyFilterName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","filter name");
            String filterValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","filter value");
            String childServiceLevelId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","cslid1");

            Boolean updateCustomCurrency = updateCustomCurrencyFields(cslEntity,Integer.parseInt(childServiceLevelId),customCurrencyFilterName,Double.valueOf(filterValue));
            Double filterValueConverted = 0.0;

            if(!updateCustomCurrency){
                customAssert.assertTrue(false,"Error while updating custom currency");
            }

            String payload = "{\"filterMap\":{\"entityTypeId\":" + cslEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"" + genericFilterId + "\":{\"filterId\":\"" + genericFilterId + "\",\"filterName\":\"" + customCurrencyFilterName + "\"," +
                    "\"entityFieldId\":" + customCurrencyFilterName + ",\"entityFieldHtmlType\":19," +
                    "\"min\":\"" + Integer.parseInt(filterValue) + "\",\"max\":\"" + Integer.parseInt(filterValue + 1) + "\",\"suffix\":null}}}," +
                    "\"selectedColumns\":[" +
                    "{\"columnId\":9704,\"columnQueryName\":\"id\"}," +
                    "{\"columnId\":9707,\"columnQueryName\":\"sl_id\"}," +
                    "{\"columnId\":13206,\"columnQueryName\":\"masterslid\"}," +
                    "{\"columnId\":113776,\"columnQueryName\":\"dyn" + customCurrencyFilterName + "\"}," +
                    "{\"columnId\":113777,\"columnQueryName\":\"clientdyn" + customCurrencyFilterName + "\"}]}";


            reportRendererListData.hitReportRendererListData(Integer.parseInt(listId),payload);

            String reportRendererListDataResponse = reportRendererListData.getListDataJsonStr();

            JSONObject reportListDataResponseJson = new JSONObject(reportRendererListDataResponse);

            JSONArray dataArray = reportListDataResponseJson.getJSONArray("data");

            if(dataArray.length() < 1){
                logger.error("Expected record size should be greater than 1 as unique filter is applied for the same ....Please correct the configuration or update the record");
                customAssert.assertTrue(false,"Expected record size should be 1 as unique filter is applied for the same ....Please correct the configuration or update the record");
            }

            JSONObject indDataRow = dataArray.getJSONObject(0);

            JSONArray indDataRowArray = JSONUtility.convertJsonOnjectToJsonArray(indDataRow);
            String columnName;
            String columnValue;
            String customCurrencyFieldValueOnReportListing = "-1";
            String convertedCustomCurrencyFieldValueOnReportListing = "-1";

            int cslId = -1;
            JSONObject indRowDataJson;

            for(int i =0;i<indDataRowArray.length();i++){

                indRowDataJson = indDataRowArray.getJSONObject(i);

                columnName = indRowDataJson.get("columnName").toString();
                columnValue = indRowDataJson.get("value").toString();

                if(columnName.equals("id")){
                    cslId = Integer.parseInt(columnValue.split(":;")[1]);
                }

                if(columnName.equals("dyn" + customCurrencyFilterName)){
                    customCurrencyFieldValueOnReportListing = columnValue;
                }

                if(columnName.equals("clientdyn" + customCurrencyFilterName)){
                    convertedCustomCurrencyFieldValueOnReportListing = columnValue;
                    if(convertedCustomCurrencyFieldValueOnReportListing.equals("0.00")){
                        convertedCustomCurrencyFieldValueOnReportListing = "0";
                    }
                }

            }

            if(cslId == -1){
                customAssert.assertTrue(false,"csl id is equal to -1");
            }else {
                Show show = new Show();
                show.hitShowVersion2(cslEntityTypeId, cslId);
                String showResponse = show.getShowJsonStr();
                JSONObject showResponseJson = new JSONObject(showResponse);

                try {
                    String convertedCustomCurrencyValueShowPage =  showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyFilterName).get("displayValues").toString();
                    String customCurrencyValueShowPage = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyFilterName).get("values").toString();

                    if(!customCurrencyValueShowPage.equals(customCurrencyFieldValueOnReportListing)){
                        customAssert.assertTrue(false,"Expected and Actual Value for custom Currency Value didn't match");
                    }

                    if(!convertedCustomCurrencyValueShowPage.contains("(" + convertedCustomCurrencyFieldValueOnReportListing + " ")){
                        customAssert.assertTrue(false,"Expected and Actual Value for converted custom Currency Value didn't match");
                    }

                }catch (Exception e){
                    customAssert.assertTrue(true,"Exception while validating field " + customCurrencyFilterName);
                }

            }
        }catch (Exception e){

            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }


        customAssert.assertAll();

    }

//    Dev Bug is there
    @Test(enabled = true)
    public void TestCSLStatusTransitionReportDownload(){

        CustomAssert customAssert = new CustomAssert();

        try{

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();

            int reportId = 406;

            String genericFilterId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","filter id for status transition report");
            String customCurrencyFilterName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","filter name");
            String filterValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","filter value");
            String childServiceLevelId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl filter","cslid");

            Boolean updateCustomCurrency = updateCustomCurrencyFields(cslEntity,Integer.parseInt(childServiceLevelId),customCurrencyFilterName,Double.valueOf(filterValue));

            if(!updateCustomCurrency){
                customAssert.assertTrue(false,"Error while updating custom currency");
            }

            String payload = "{\"filterMap\":{\"entityTypeId\":15,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"" + genericFilterId + "\":{\"filterId\":\"" + genericFilterId + "\",\"" +
                    "filterName\":\"" + customCurrencyFilterName + "\",\"entityFieldId\":" + customCurrencyFilterName + "," +
                    "\"entityFieldHtmlType\":19,\"min\":\"" + filterValue + "\",\"max\":\"" + filterValue + "\",\"suffix\":null}}}}";

            Map<String,String> formParam = new HashMap<>();
            formParam.put("_csrf_token","null");
            formParam.put("jsonData",payload);

            HttpResponse downloadResponse =  downloadReportWithData.hitDownloadReportWithData(formParam,reportId);

            String outputFileName = "CSL Status Transition Report.xlsx";

            Boolean downloadStatus = dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath,outputFileName);

            if(!(downloadStatus == true)){
                customAssert.assertTrue(false,"Status Transition Report Download unsuccessfully");
            }else {
                String sheetName = "Data";

                int customCurrencyColumnNoExcel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"status transition report","custom currency column no excel"));
                int convertedCustomCurrencyColumnNoExcel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"status transition report","conv custom currency column no excel"));

                String cslIdExcel = XLSUtils.getOneCellValue(outputFilePath,outputFileName,sheetName,6,0);

                if(cslIdExcel == null){
                    customAssert.assertTrue(false,"From the Status Transition Report Downloaded the value of CSL Id is null...Please correct the filter or it is an issue");
                    customAssert.assertAll();
                }
                String cslIdSeqId = cslIdExcel.split("CSL")[1];

                if(cslIdSeqId.charAt(0) == 0){
                    cslIdSeqId = cslIdSeqId.substring(1);
                }

                String sqlQuery = "select id from child_sla where client_entity_seq_id  = '" + cslIdSeqId + "'";

                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName, dbName,dbUserName,dbPassowrd);

                List<List<String>> sqlQueryResult = postgreSQLJDBC.doSelect(sqlQuery);

                int cslId = Integer.parseInt(sqlQueryResult.get(0).get(0));

                String customCurrencyColumnValueExcel = XLSUtils.getOneCellValue(outputFilePath,outputFileName,sheetName,6,customCurrencyColumnNoExcel);
                String conCustomCurrencyColumnValueExcel = XLSUtils.getOneCellValue(outputFilePath,outputFileName,sheetName,6,convertedCustomCurrencyColumnNoExcel);

                Show show = new Show();
                show.hitShowVersion2(cslEntityTypeId, cslId);
                String showResponse = show.getShowJsonStr();
                JSONObject showResponseJson = new JSONObject(showResponse);

                try {
                    String convertedCustomCurrencyValueShowPage =  showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyFilterName).get("displayValues").toString();
                    String customCurrencyValueShowPage = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyFilterName).get("values").toString();

                    if(!customCurrencyValueShowPage.equals(customCurrencyColumnValueExcel)){
                        customAssert.assertTrue(false,"Expected and Actual Value for custom Currency Value didn't match");
                    }

                    if(!convertedCustomCurrencyValueShowPage.contains("(" + conCustomCurrencyColumnValueExcel + " ")){
                        customAssert.assertTrue(false,"Expected and Actual Value for converted custom Currency Value didn't match");
                    }

                }catch (Exception e){
                    customAssert.assertTrue(true,"Exception while validating field " + customCurrencyFilterName);
                }


            }

        }catch (Exception e){

            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }


        customAssert.assertAll();

    }

    @Test
    public void TestSLStatusDetailsReportDownload(){

        CustomAssert customAssert = new CustomAssert();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName, dbName,dbUserName,dbPassowrd);

        try{

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();

            int reportId = 59;

            String genericFilterId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl filter","filter id for sl details report");
            String customCurrencyFilterName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl filter","filter name");
            String filterValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl filter","filter value");
            String serviceLevelId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl filter","slid");

            Boolean updateCustomCurrency = updateCustomCurrencyFields(slEntity,Integer.parseInt(serviceLevelId),customCurrencyFilterName,Double.valueOf(filterValue));

            if(!updateCustomCurrency){
                customAssert.assertTrue(false,"Service Level edited unsuccessfully");
            }

            String payload = "{\"filterMap\":{\"entityTypeId\":14,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"" + genericFilterId + "\":{\"filterId\":\"" + genericFilterId + "\"," +
                    "\"filterName\":\"" + customCurrencyFilterName + "\",\"entityFieldId\":" + customCurrencyFilterName + "," +
                    "\"entityFieldHtmlType\":19,\"min\":\"" + filterValue + "\",\"max\":\"" + filterValue + "\",\"suffix\":null}}}}";

            Map<String,String> formParam = new HashMap<>();
            formParam.put("_csrf_token","null");
            formParam.put("jsonData",payload);

            HttpResponse downloadResponse =  downloadReportWithData.hitDownloadReportWithData(formParam,reportId);

            String outputFileName = "MSL - Details.xlsx";

            Boolean downloadStatus = dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath,outputFileName);

            if(!(downloadStatus == true)){
                customAssert.assertTrue(false,"Status Transition Report Download unsuccessfully");
            }else {
                String sheetName = "SL DETAIL";

                int customCurrencyColumnNoExcel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"msl details report","custom currency column no excel"));
                int convertedCustomCurrencyColumnNoExcel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"msl details report","conv custom currency column no excel"));

                String slIdExcel = XLSUtils.getOneCellValue(outputFilePath,outputFileName,sheetName,6,0);

                String cslIdSeqId = slIdExcel.split("SL")[1];

                if(cslIdSeqId.charAt(0) == '0'){
                    cslIdSeqId = cslIdSeqId.substring(1);
                }

                String sqlQuery = "select id from sla where client_entity_seq_id  = '" + cslIdSeqId + "'";

                List<List<String>> sqlQueryResult = postgreSQLJDBC.doSelect(sqlQuery);

                int slId = Integer.parseInt(sqlQueryResult.get(0).get(0));

                String customCurrencyColumnValueExcel = XLSUtils.getOneCellValue(outputFilePath,outputFileName,sheetName,6,customCurrencyColumnNoExcel);
                String conCustomCurrencyColumnValueExcel = XLSUtils.getOneCellValue(outputFilePath,outputFileName,sheetName,6,convertedCustomCurrencyColumnNoExcel);

                Show show = new Show();
                show.hitShowVersion2(slEntityTypeId, slId);
                String showResponse = show.getShowJsonStr();
                JSONObject showResponseJson = new JSONObject(showResponse);

                try {
                    String convertedCustomCurrencyValueShowPage =  showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyFilterName).get("displayValues").toString();
                    String customCurrencyValueShowPage = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyFilterName).get("values").toString();

                    if(!customCurrencyValueShowPage.equals(customCurrencyColumnValueExcel)){
                        customAssert.assertTrue(false,"Expected and Actual Value for custom Currency Value didn't match");
                    }

                    if(!convertedCustomCurrencyValueShowPage.contains("(" + conCustomCurrencyColumnValueExcel + " ")){
                        customAssert.assertTrue(false,"Expected and Actual Value for converted custom Currency Value didn't match");
                    }

                }catch (Exception e){
                    customAssert.assertTrue(true,"Exception while validating field " + customCurrencyFilterName);
                }


            }



        }catch (Exception e){

            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }finally {
            postgreSQLJDBC.closeConnection();
        }


        customAssert.assertAll();

    }

    private Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String outputFileName) {

        Boolean status;

        FileUtils fileUtil = new FileUtils();

        String  outputFile = outputFilePath + "/" + outputFileName;
        status = fileUtil.writeResponseIntoFile(response, outputFile);
        if (status)
            logger.info("DownloadListWithData file generated at {}", outputFile);

        return status;
    }

    private Boolean updateCustomCurrencyFields(String entityName,int entityId,String customCurrencyField,Double customCurrencyFieldValue) {


        Boolean editStatus = true;
        Edit edit = new Edit();
        String editResponse;
        try {

            editResponse = edit.hitEdit(entityName, entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            if(customCurrencyField !=null) {

                JSONObject dynamicValuesJson = new JSONObject();
                dynamicValuesJson.put("name","dyn" + customCurrencyField);
                dynamicValuesJson.put("id",customCurrencyField);
                dynamicValuesJson.put("values",customCurrencyFieldValue);

                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").put("dyn" + customCurrencyField,dynamicValuesJson);
                editResponseJson.getJSONObject("body").getJSONObject("data").put("dyn" + customCurrencyField, dynamicValuesJson);


            }
            editResponse = edit.hitEdit(entityName, editResponseJson.toString());

            if (!editResponse.contains("success")) {

                editStatus = false;
            }

        } catch (Exception e) {
            logger.error("Exception while Editing custom currency Fields  " + e.getMessage());
            editStatus = false;
        }


        return editStatus;
    }
}
