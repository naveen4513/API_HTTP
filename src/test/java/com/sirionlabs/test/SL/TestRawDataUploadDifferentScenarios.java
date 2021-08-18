package com.sirionlabs.test.SL;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;

import com.sirionlabs.utils.commonUtils.*;

import org.apache.velocity.runtime.directive.Parse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestRawDataUploadDifferentScenarios {

    private final static Logger logger = LoggerFactory.getLogger(TestRawDataUploadDifferentScenarios.class);

    int slId;
    String slConfigFilePath;
    String slConfigFileName;
    String uploadFilePath;

    int uploadIdSL_PerformanceDataTab;
    int slMetaDataUploadTemplateId;

    int cslEntityTypeId = 15;

    String dbHostName;
    String dbPortName;
    String dbName;
    String dbUserName;
    String dbPassowrd;

    @BeforeClass
    public void BeforeClass(){

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"raw data file upload different scenarios","sl id"));
        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadidslperformancecdatatab"));
        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slmetadatauploadtemplateid"));

        uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\RawDataUploadDifferentScenarios";

        dbHostName = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassowrd = ConfigureEnvironment.getEnvironmentProperty("dbPassword");
    }

    @Test(enabled = true)
    public void TestUploadRawDataWithMisColAccToTemplate(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        try{


            String performanceDataFormatFileName = "PerformanceDataFormat.xlsm";
            String expectedMsg = "200";

            Boolean uploadSLTemplate = serviceLevelHelper.uploadPerformanceDataFormat(slId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert);

            if(!uploadSLTemplate){
                customAssert.assertTrue(false,"Error while uploading SL Template");
            }

            Boolean fileUploadStatus = serviceLevelHelper.validatePerformanceDataFormatTab(slId,performanceDataFormatFileName,customAssert);

            if(!fileUploadStatus){
                customAssert.assertTrue(false,"SL Template not uploaded successfully");
            }

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"raw data file upload different scenarios","csl id"));

            String rawDataFile = "RawDataFileColumnMissing.xlsx";

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean cslRawDataUpload = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,rawDataFile,expectedMsg,customAssert);

            if(!cslRawDataUpload){
                customAssert.assertTrue(false,"Error while uploading Raw Data");
            }

            Thread.sleep(3000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("name", showResponse);


            String subjectLine = "Error in pre-processing raw data file (" + shortCodeId + " - " + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

            List<String> expectedSentencesInBody = new ArrayList<>();
            expectedSentencesInBody.add("Incorrect headers");


            if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
                customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario  " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestUploadRawDataWithExtraColAccToTemplate(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        try{


            String performanceDataFormatFileName = "PerformanceDataFormat.xlsm";
            String expectedMsg = "200";

            Boolean uploadSLTemplate = serviceLevelHelper.uploadPerformanceDataFormat(slId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert);

            if(!uploadSLTemplate){
                customAssert.assertTrue(false,"Error while uploading SL Template");
            }

            Boolean fileUploadStatus = serviceLevelHelper.validatePerformanceDataFormatTab(slId,performanceDataFormatFileName,customAssert);

            if(!fileUploadStatus){
                customAssert.assertTrue(false,"SL Template not uploaded successfully");
            }

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"raw data file upload different scenarios","csl id"));

            String rawDataFile = "RawDataFileColumnExtra.xlsx";

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean cslRawDataUpload = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,rawDataFile,expectedMsg,customAssert);

            if(!cslRawDataUpload){
                customAssert.assertTrue(false,"Error while uploading Raw Data");
            }

            Thread.sleep(3000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("name", showResponse);


            String subjectLine = "Error in pre-processing raw data file (" + shortCodeId + " - " + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

            List<String> expectedSentencesInBody = new ArrayList<>();
            expectedSentencesInBody.add("Incorrect headers");


            if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
                customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario");
            logger.error("Exception while validating the scenario  " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestUploadRawDataWithZeroRecord(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        try{


            String performanceDataFormatFileName = "PerformanceDataFormat.xlsm";
            String expectedMsg = "200";

            Boolean uploadSLTemplate = serviceLevelHelper.uploadPerformanceDataFormat(slId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert);

            if(!uploadSLTemplate){
                customAssert.assertTrue(false,"Error while uploading SL Template");
            }

            Boolean fileUploadStatus = serviceLevelHelper.validatePerformanceDataFormatTab(slId,performanceDataFormatFileName,customAssert);

            if(!fileUploadStatus){
                customAssert.assertTrue(false,"SL Template not uploaded successfully");
            }

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"raw data file upload different scenarios","csl id"));

            String rawDataFile = "RawDataFileZeroRecord.xlsx";

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean cslRawDataUpload = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,rawDataFile,expectedMsg,customAssert);

            if(!cslRawDataUpload){
                customAssert.assertTrue(false,"Error while uploading Raw Data");
            }

            Thread.sleep(6000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("name", showResponse);

            String subjectLine = "Error in pre-processing raw data file (" + shortCodeId + " - " + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

            List<String> expectedSentencesInBody = new ArrayList<>();
            expectedSentencesInBody.add("Does Not Contain Minimum Number Of Rows Allowed To Upload");


            if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
                customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario  " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestUploadRawDataWithUnSupportedFormatInColumn(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        try{


            String performanceDataFormatFileName = "PerformanceDataFormat.xlsm";
            String expectedMsg = "200";

            Boolean uploadSLTemplate = serviceLevelHelper.uploadPerformanceDataFormat(slId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert);

            if(!uploadSLTemplate){
                customAssert.assertTrue(false,"Error while uploading SL Template");
            }

            Boolean fileUploadStatus = serviceLevelHelper.validatePerformanceDataFormatTab(slId,performanceDataFormatFileName,customAssert);

            if(!fileUploadStatus){
                customAssert.assertTrue(false,"SL Template not uploaded successfully");
            }

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"raw data file upload different scenarios","csl id"));
//            int cslId = 516669;

            String rawDataFile = "RawDataFileWrongFormatInColumn.xlsx";

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean cslRawDataUpload = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,rawDataFile,expectedMsg,customAssert);

            if(!cslRawDataUpload){
                customAssert.assertTrue(false,"Error while uploading Raw Data");
            }

            Thread.sleep(6000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("name", showResponse);

            String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";
            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

            List<String> expectedSentencesInBody = new ArrayList<>();
            expectedSentencesInBody.add("Error Type: Data Type Mismatch");


            if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
                customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario  " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestChangeInTemplateFormatAlreadySubmitted(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        try{


            String performanceDataFormatFileNameNew = "PerformanceDataFormatNewTemplate.xlsm";
            String performanceDataFormatFileNameOld = "PerformanceDataFormat.xlsm";

            String expectedMsg = "200";

            Boolean uploadSLTemplate = serviceLevelHelper.uploadPerformanceDataFormat(slId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileNameNew, expectedMsg, customAssert);

            if(!uploadSLTemplate){
                customAssert.assertTrue(false,"Error while uploading SL Template");
            }

            Boolean fileUploadStatus = serviceLevelHelper.validatePerformanceDataFormatTab(slId,performanceDataFormatFileNameNew,customAssert);

            if(!fileUploadStatus){
                customAssert.assertTrue(false,"SL Template not uploaded successfully");
            }

            String excelSheetNameRawData = "Format Sheet";
            int startingRowNum = 2;

            List<List<String>> viewTemplateListExcel = prepareViewTemplateListExcel(uploadFilePath, performanceDataFormatFileNameOld, excelSheetNameRawData, startingRowNum, 5,customAssert);

            int cSlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"raw data file upload different scenarios","csl id already submitted"));

            if (!validateViewTemplate(cSlId, viewTemplateListExcel, customAssert)) {
                customAssert.assertTrue(false, "View Template functionality validated unsuccessfully for CSL ID " + cSlId + " using excel template " + performanceDataFormatFileNameOld);
            }



        }catch (Exception e){
            logger.error("Exception while validating the scenario  " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestChangeInTemplateFormatNotSubmittedNotIndexed(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        try{


            String performanceDataFormatFileNameNew = "PerformanceDataFormatNewTemplate.xlsm";
            String performanceDataFormatFileNameOld = "PerformanceDataFormat.xlsm";

            String expectedMsg = "200";

            Boolean uploadSLTemplate = serviceLevelHelper.uploadPerformanceDataFormat(slId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileNameNew, expectedMsg, customAssert);

            if(!uploadSLTemplate){
                customAssert.assertTrue(false,"Error while uploading SL Template");
            }

            Boolean fileUploadStatus = serviceLevelHelper.validatePerformanceDataFormatTab(slId,performanceDataFormatFileNameNew,customAssert);

            if(!fileUploadStatus){
                customAssert.assertTrue(false,"SL Template not uploaded successfully");
            }

            String excelSheetNameRawData = "Format Sheet";
            int startingRowNum = 2;

            List<List<String>> viewTemplateListExcel = prepareViewTemplateListExcel(uploadFilePath, performanceDataFormatFileNameNew, excelSheetNameRawData, startingRowNum, 5,customAssert);

            int cSlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"raw data file upload different scenarios","csl id not submitted not indexed"));
//            int cSlId = 516671;
            if (!validateViewTemplate(cSlId, viewTemplateListExcel, customAssert)) {
                customAssert.assertTrue(false, "View Template functionality validated unsuccessfully for CSL ID " + cSlId + " using excel template " + performanceDataFormatFileNameOld);
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario  " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }


    private List<List<String>> getRecordFromSystemEmailTable(String subjectLine, String currentTimeStamp) {

        String sqlQuery = "select subject,sent_successfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
                + "order by id desc";
        List<List<String>> queryResult = null;
        PostgreSQLJDBC postgreSQLJDBC;
        dbName = "letterbox-sl";

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);

        try {
            queryResult = postgreSQLJDBC.doSelect(sqlQuery);

        } catch (Exception e) {
            logger.error("Exception while getting record from sql " + e.getMessage());
        }finally {
			postgreSQLJDBC.closeConnection();
        }
        return queryResult;

    }

    private List<List<String>> getCurrentTimeStamp() {

        String sqlString = "select current_timestamp";
        List<List<String>> currentTimeStamp = null;
        PostgreSQLJDBC postgreSQLJDBC;

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);
        try {


            currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
        } catch (Exception e) {
            logger.error("Exception while getting current time stamp " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return currentTimeStamp;
    }


    private Boolean validateBodyOfEmail(List<List<String>> recordFromSystemEmailTable, List<String> expectedStringInBody, CustomAssert customAssert) {

        Boolean validationStatus = true;

        String actualBodyHtml;
        try {

            for (int i = 0; i < recordFromSystemEmailTable.size(); i++) {
                actualBodyHtml = recordFromSystemEmailTable.get(i).get(2);

                for (int j = 0; j < expectedStringInBody.size(); j++) {

                    if (!actualBodyHtml.contains(expectedStringInBody.get(j))) {
                        customAssert.assertTrue(false, "While validating email Body Html does not contain the expected String " + expectedStringInBody.get(j));
                        validationStatus = false;
                    }
                }


            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating body of Email");
            validationStatus = false;
        }

        return validationStatus;
    }

    private List<List<String>> prepareViewTemplateListExcel(String excelFilePath, String excelFileName, String sheetName, int startingRowNum,int numberOfColumns, CustomAssert customAssert) {

        logger.info("Preparing excel List for View Template validation");
        List<List<String>> viewTemplateListExcel = new ArrayList<>();

        try {
            int numOfRows = Integer.parseInt(XLSUtils.getNoOfRows(excelFilePath, excelFileName, sheetName).toString());
            viewTemplateListExcel = XLSUtils.getExcelDataOfMultipleRowsWithNullAsHyphenInAnyColumn(excelFilePath, excelFileName, sheetName, startingRowNum, numOfRows,numberOfColumns);
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while preparing View Template List From Excel FileName : " + excelFileName);
            viewTemplateListExcel.clear();
        }
        return viewTemplateListExcel;
    }

    private Boolean validateViewTemplate(int cslID, List<List<String>> viewTemplateListExcel, CustomAssert customAssert) {

        logger.info("Validating View template on CSL Id " + cslID);
        Boolean validationStatus = true;
        try {

            SLDetails slDetails = new SLDetails();
            String payload = "{\"filterMap\":{\"entityTypeId\":1,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            slDetails.hitViewTemplate(cslID, payload);
            String slDetailsResponse = slDetails.getSLDetailsResponseStr();

            if (JSONUtility.validjson(slDetailsResponse)) {

                JSONObject slDetailsResponseJson = new JSONObject(slDetailsResponse);
                JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");
                JSONArray indvRowArray;

                String columnValue;

                List<String> indvRowColumnValuesList;
                List<List<String>> viewTemplateOnScreenDataList = new ArrayList<>();

                if (viewTemplateListExcel == null) {
                    if (dataArray.length() == 0) {
                        customAssert.assertTrue(true, "When no template is uploaded on service level then view template has no data");
                        return true;
                    } else {
                        customAssert.assertTrue(false, "When no template is uploaded on service level then view template has data on child service level on pressing view template button for CSL ID " + cslID);
                        return false;
                    }
                }

                for (int i = 0; i < dataArray.length(); i++) {

                    indvRowArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));
                    indvRowColumnValuesList = new ArrayList();
                    for (int j = 0; j < indvRowArray.length(); j++) {

                        columnValue = indvRowArray.getJSONObject(j).get("columnValue").toString();
                        if (columnValue.equalsIgnoreCase("null")) {
                            columnValue = "-";
                        }
                        indvRowColumnValuesList.add(columnValue);
                    }
                    viewTemplateOnScreenDataList.add(indvRowColumnValuesList);
                }

                //Validating List Prepared from On Screen With the view Template from Excel
                List<String> excelListIndvRow;
                List<String> screenListIndvRow;

                if (viewTemplateOnScreenDataList.size() == viewTemplateListExcel.size()) {
                    for (int i = 0; i < viewTemplateOnScreenDataList.size(); i++) {

                        excelListIndvRow = viewTemplateListExcel.get(i);
                        screenListIndvRow = viewTemplateOnScreenDataList.get(i);

                        if (excelListIndvRow.size() == screenListIndvRow.size()) {

                            for (int j = 0; j < excelListIndvRow.size(); j++) {

                                if (!excelListIndvRow.get(j).equalsIgnoreCase(screenListIndvRow.get(j))) {
                                    customAssert.assertTrue(false, "Value from template and Value on Screen are not equal for row number " + i + " and column number " + j);
                                    customAssert.assertTrue(false, "Template value from Excel " + excelListIndvRow.get(j) + "Template Value On Screen " + screenListIndvRow.get(j));
                                    validationStatus = false;
                                }
                            }
                        } else {
                            customAssert.assertTrue(false, "Number of columns from excel and On Template Format Screen are unequal for CSL ID " + cslID);
                            validationStatus = false;
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Number of Rows from excel and On Template Format Screen are unequal for CSL ID " + cslID);
                    validationStatus = false;
                }

            } else {
                customAssert.assertTrue(false, "API Response for View Template is not a valid Json for CSL ID " + cslID);
                validationStatus = false;
                return validationStatus;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating view template on CSL ID " + cslID + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;
    }
}
