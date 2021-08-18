package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldLabelsOnReports extends TestDisputeInternationalization{
    private static Map<String, String> reportIdMap = new HashMap<>();
    private static int globalIndex = 0;
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnReports.class);
    public void verifyFieldLabelsOnReports(String entityName, CustomAssert csAssert) throws ConfigurationException {
        String baseFilePath = "src//test//resources//CommonConfigFiles";
        String reportIdFile = "ReportListId.cfg";
        reportIdMap = ParseConfigFile.getAllConstantProperties(baseFilePath, reportIdFile, entityName);
        for ( String ReportID : reportIdMap.values() ) {
            try {
                logger.info("Validating Field Labels on List Page on Report of" + entityName);
                ReportRendererDefaultUserListMetaData defaultObj = new ReportRendererDefaultUserListMetaData();
                defaultObj.hitReportRendererDefaultUserListMetadata(Integer.parseInt(ReportID));
                String defaultUserListResponse = defaultObj.getReportRendererDefaultUserListMetaDataJsonStr();
                if (!ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
                    logger.error("DefaultUserListMetaData API Response for List Id {} and EntityTypeId {} is an Invalid JSON.", ReportID, entityTypeId);
                    return;
                }
                JSONObject defaultJsonObj = new JSONObject(defaultUserListResponse);
                JSONArray jsonArrColName = defaultJsonObj.getJSONArray("columns");
                for ( int i = 0; i < jsonArrColName.length(); i++ ) {
                    String colName = jsonArrColName.getJSONObject(i).getString("name").trim();
                    if (colName.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                        csAssert.assertTrue(false, "Field Label: [" + colName.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under listing columns of " +entityName +" report with report ID: " +ReportID);
                    } else {
                        csAssert.assertTrue(true, "Field Label: [" + colName + "] does not contain: [" + expectedPostFix + "] under listing columns of " +entityName +"report with report ID: " +ReportID);
                    }
                }
                JSONArray jsonArrFilterName = defaultJsonObj.getJSONArray("filterMetadatas");
                for ( int i = 0; i < jsonArrFilterName.length(); i++ ) {
                    String filterName = jsonArrFilterName.getJSONObject(i).getString("name").trim();

                    if (filterName.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                        csAssert.assertTrue(false, "Field Label: [" + filterName.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under filters of " + entityName +" report with report ID: " +ReportID);
                    } else {
                        csAssert.assertTrue(true, "Field Label: [" + filterName + "] does not contain:" + expectedPostFix + "]  under filters of " + entityName +" report with report ID: " +ReportID);
                    }
                }
                if(entityName.toLowerCase().contains("suppliers")) {
                    testDownloadListWithData(entityName, 1, 48);
                    ReportListDownloadExcelHearderLableMatch(entityName, 48,csAssert);
                }else if(entityName.toLowerCase().contains("obligations")) {
                    testDownloadListWithData(entityName, 12, 58);
                    ReportListDownloadExcelHearderLableMatch(entityName, 58,csAssert);
                    testDownloadListWithData(entityName, 12, 153);
                    ReportListDownloadExcelHearderLableMatch(entityName, 153,csAssert);
                    testDownloadListWithData(entityName, 12, 1001);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1001,csAssert);
                }else if(entityName.toLowerCase().contains("child obligations")) {
                    testDownloadListWithData(entityName, 13, 22);
                    ReportListDownloadExcelHearderLableMatch(entityName, 22,csAssert);
                    testDownloadListWithData(entityName, 13, 27);
                    ReportListDownloadExcelHearderLableMatch(entityName, 27,csAssert);
                    testDownloadListWithData(entityName, 13, 18);
                    ReportListDownloadExcelHearderLableMatch(entityName, 18,csAssert);
                    testDownloadListWithData(entityName, 13, 83);
                    ReportListDownloadExcelHearderLableMatch(entityName, 83,csAssert);
                    testDownloadListWithData(entityName, 13, 19);
                    ReportListDownloadExcelHearderLableMatch(entityName, 19,csAssert);
                    testDownloadListWithData(entityName, 13, 82);
                    ReportListDownloadExcelHearderLableMatch(entityName, 82,csAssert);
                    testDownloadListWithData(entityName, 13, 26);
                    ReportListDownloadExcelHearderLableMatch(entityName, 26,csAssert);
                    testDownloadListWithData(entityName, 13, 1009);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1009,csAssert);
                    testDownloadListWithData(entityName, 13, 16);
                    ReportListDownloadExcelHearderLableMatch(entityName, 16,csAssert);
                    testDownloadListWithData(entityName, 13, 15);
                    ReportListDownloadExcelHearderLableMatch(entityName, 15,csAssert);
                    testDownloadListWithData(entityName, 13, 439);
                    ReportListDownloadExcelHearderLableMatch(entityName, 439,csAssert);
                    testDownloadListWithData(entityName, 13, 13);
                    ReportListDownloadExcelHearderLableMatch(entityName, 13,csAssert);
                    testDownloadListWithData(entityName, 13, 20);
                    ReportListDownloadExcelHearderLableMatch(entityName, 20,csAssert);
                    testDownloadListWithData(entityName, 13, 14);
                    ReportListDownloadExcelHearderLableMatch(entityName, 14,csAssert);
                    testDownloadListWithData(entityName, 13, 17);
                    ReportListDownloadExcelHearderLableMatch(entityName, 17,csAssert);
                    testDownloadListWithData(entityName, 13, 21);
                    ReportListDownloadExcelHearderLableMatch(entityName, 21,csAssert);
                }else if(entityName.toLowerCase().contains("service levels")) {
                    testDownloadListWithData(entityName, 14, 1010);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1010,csAssert);
                    testDownloadListWithData(entityName, 14, 59);
                    ReportListDownloadExcelHearderLableMatch(entityName, 59,csAssert);
                    testDownloadListWithData(entityName, 14, 330);
                    ReportListDownloadExcelHearderLableMatch(entityName, 330,csAssert);
                }else if(entityName.toLowerCase().contains("child service levels")) {
                    testDownloadListWithData(entityName, 15, 86);
                    ReportListDownloadExcelHearderLableMatch(entityName, 86,csAssert);
                    testDownloadListWithData(entityName, 15, 282);
                    ReportListDownloadExcelHearderLableMatch(entityName, 282,csAssert);
                    testDownloadListWithData(entityName, 15, 281);
                    ReportListDownloadExcelHearderLableMatch(entityName, 281,csAssert);
                    testDownloadListWithData(entityName, 15, 57);
                    ReportListDownloadExcelHearderLableMatch(entityName, 57,csAssert);
                    testDownloadListWithData(entityName, 15, 345);
                    ReportListDownloadExcelHearderLableMatch(entityName, 345,csAssert);
                    testDownloadListWithData(entityName, 15, 1005);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1005,csAssert);
                    testDownloadListWithData(entityName, 15, 91);
                    ReportListDownloadExcelHearderLableMatch(entityName, 91,csAssert);
                    testDownloadListWithData(entityName, 15, 87);
                    ReportListDownloadExcelHearderLableMatch(entityName, 87,csAssert);
                    testDownloadListWithData(entityName, 15, 92);
                    ReportListDownloadExcelHearderLableMatch(entityName, 92,csAssert);
                    testDownloadListWithData(entityName, 15, 406);
                    ReportListDownloadExcelHearderLableMatch(entityName, 406,csAssert);
                }else if(entityName.toLowerCase().contains("interpretations")) {
                    testDownloadListWithData(entityName, 16, 54);
                    ReportListDownloadExcelHearderLableMatch(entityName, 54,csAssert);
                    testDownloadListWithData(entityName, 16, 37);
                    ReportListDownloadExcelHearderLableMatch(entityName, 37,csAssert);
                    testDownloadListWithData(entityName, 16, 56);
                    ReportListDownloadExcelHearderLableMatch(entityName, 56,csAssert);
                    testDownloadListWithData(entityName, 16, 1004);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1004,csAssert);
                    testDownloadListWithData(entityName, 16, 55);
                    ReportListDownloadExcelHearderLableMatch(entityName, 55,csAssert);
                    testDownloadListWithData(entityName, 16, 53);
                    ReportListDownloadExcelHearderLableMatch(entityName, 53,csAssert);
                }else if(entityName.toLowerCase().contains("issues")) {
                    testDownloadListWithData(entityName, 17, 39);
                    ReportListDownloadExcelHearderLableMatch(entityName, 39,csAssert);
                    testDownloadListWithData(entityName, 17, 38);
                    ReportListDownloadExcelHearderLableMatch(entityName, 38,csAssert);
                    testDownloadListWithData(entityName, 17, 205);
                    ReportListDownloadExcelHearderLableMatch(entityName, 205,csAssert);
                    testDownloadListWithData(entityName, 17, 40);
                    ReportListDownloadExcelHearderLableMatch(entityName, 40,csAssert);
                    testDownloadListWithData(entityName, 17, 43);
                    ReportListDownloadExcelHearderLableMatch(entityName, 43,csAssert);
                    testDownloadListWithData(entityName, 17, 41);
                    ReportListDownloadExcelHearderLableMatch(entityName, 41,csAssert);
                    testDownloadListWithData(entityName, 17, 204);
                    ReportListDownloadExcelHearderLableMatch(entityName, 204,csAssert);
                    testDownloadListWithData(entityName, 17, 46);
                    ReportListDownloadExcelHearderLableMatch(entityName, 46,csAssert);
                    testDownloadListWithData(entityName, 17, 45);
                    ReportListDownloadExcelHearderLableMatch(entityName, 45,csAssert);
                    testDownloadListWithData(entityName, 17, 44);
                    ReportListDownloadExcelHearderLableMatch(entityName, 44,csAssert);
                    testDownloadListWithData(entityName, 17, 42);
                    ReportListDownloadExcelHearderLableMatch(entityName, 42,csAssert);
                    testDownloadListWithData(entityName, 17, 1002);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1002,csAssert);
                    testDownloadListWithData(entityName, 17, 36);
                    ReportListDownloadExcelHearderLableMatch(entityName, 36,csAssert);
                    testDownloadListWithData(entityName, 17, 47);
                    ReportListDownloadExcelHearderLableMatch(entityName, 47,csAssert);
                    testDownloadListWithData(entityName, 17, 206);
                    ReportListDownloadExcelHearderLableMatch(entityName, 206,csAssert);
                }else if(entityName.toLowerCase().contains("actions")) {
                    testDownloadListWithData(entityName, 18, 440);
                    ReportListDownloadExcelHearderLableMatch(entityName, 440,csAssert);
                    testDownloadListWithData(entityName, 18, 30);
                    ReportListDownloadExcelHearderLableMatch(entityName, 30,csAssert);
                    testDownloadListWithData(entityName, 18, 25);
                    ReportListDownloadExcelHearderLableMatch(entityName, 25,csAssert);
                    testDownloadListWithData(entityName, 18, 31);
                    ReportListDownloadExcelHearderLableMatch(entityName, 31,csAssert);
                    testDownloadListWithData(entityName, 18, 32);
                    ReportListDownloadExcelHearderLableMatch(entityName, 32,csAssert);
                    testDownloadListWithData(entityName, 18, 28);
                    ReportListDownloadExcelHearderLableMatch(entityName, 28,csAssert);
                    testDownloadListWithData(entityName, 18, 33);
                    ReportListDownloadExcelHearderLableMatch(entityName, 33,csAssert);
                    testDownloadListWithData(entityName, 18, 1003);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1003,csAssert);
                    testDownloadListWithData(entityName, 18, 29);
                    ReportListDownloadExcelHearderLableMatch(entityName, 29,csAssert);
                    testDownloadListWithData(entityName, 18, 100);
                    ReportListDownloadExcelHearderLableMatch(entityName, 100,csAssert);
                    testDownloadListWithData(entityName, 18, 35);
                    ReportListDownloadExcelHearderLableMatch(entityName, 35,csAssert);
                    testDownloadListWithData(entityName, 18, 101);
                    ReportListDownloadExcelHearderLableMatch(entityName, 101,csAssert);
                    testDownloadListWithData(entityName, 18, 24);
                    ReportListDownloadExcelHearderLableMatch(entityName, 24,csAssert);
                    testDownloadListWithData(entityName, 18, 34);
                    ReportListDownloadExcelHearderLableMatch(entityName, 34,csAssert);
                    testDownloadListWithData(entityName, 18, 23);
                    ReportListDownloadExcelHearderLableMatch(entityName, 23,csAssert);
                }else if(entityName.toLowerCase().contains("disputes")) {
                    testDownloadListWithData(entityName, 28, 383);
                    ReportListDownloadExcelHearderLableMatch(entityName, 383,csAssert);
                    testDownloadListWithData(entityName, 28, 315);
                    ReportListDownloadExcelHearderLableMatch(entityName, 315,csAssert);
                }else if(entityName.toLowerCase().contains("contracts")) {
                    testDownloadListWithData(entityName, 61, 222);
                    ReportListDownloadExcelHearderLableMatch(entityName, 222, csAssert);
                    testDownloadListWithData(entityName, 61, 50);
                    ReportListDownloadExcelHearderLableMatch(entityName, 50,csAssert);
                    testDownloadListWithData(entityName, 61, 49);
                    ReportListDownloadExcelHearderLableMatch(entityName, 49,csAssert);
                    testDownloadListWithData(entityName, 61, 270);
                    ReportListDownloadExcelHearderLableMatch(entityName, 270,csAssert);
                    testDownloadListWithData(entityName, 61, 416);
                    ReportListDownloadExcelHearderLableMatch(entityName, 416,csAssert);
                    testDownloadListWithData(entityName, 61, 280);
                    ReportListDownloadExcelHearderLableMatch(entityName, 280,csAssert);
                    testDownloadListWithData(entityName, 61, 224);
                    ReportListDownloadExcelHearderLableMatch(entityName, 224,csAssert);
                    testDownloadListWithData(entityName, 61, 223);
                    ReportListDownloadExcelHearderLableMatch(entityName, 223,csAssert);
                    testDownloadListWithData(entityName, 61, 1000);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1000,csAssert);
                }else if(entityName.toLowerCase().contains("change requests")) {
                    testDownloadListWithData(entityName, 63, 151);
                    ReportListDownloadExcelHearderLableMatch(entityName, 151, csAssert);
                    testDownloadListWithData(entityName, 63, 89);
                    ReportListDownloadExcelHearderLableMatch(entityName, 89, csAssert);
                    testDownloadListWithData(entityName, 63, 152);
                    ReportListDownloadExcelHearderLableMatch(entityName, 152, csAssert);
                    testDownloadListWithData(entityName, 63, 95);
                    ReportListDownloadExcelHearderLableMatch(entityName, 95, csAssert);
                    testDownloadListWithData(entityName, 63, 1007);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1007, csAssert);
                    testDownloadListWithData(entityName, 63, 378);
                    ReportListDownloadExcelHearderLableMatch(entityName, 378, csAssert);
                    testDownloadListWithData(entityName, 63, 408);
                    ReportListDownloadExcelHearderLableMatch(entityName, 408, csAssert);
                    testDownloadListWithData(entityName, 63, 52);
                    ReportListDownloadExcelHearderLableMatch(entityName, 52, csAssert);
                    testDownloadListWithData(entityName, 63, 88);
                    ReportListDownloadExcelHearderLableMatch(entityName, 88, csAssert);
                }else if(entityName.toLowerCase().contains("invoices")) {
                    testDownloadListWithData(entityName, 67, 80);
                    ReportListDownloadExcelHearderLableMatch(entityName, 80,csAssert);
                    testDownloadListWithData(entityName, 67, 79);
                    ReportListDownloadExcelHearderLableMatch(entityName, 79,csAssert);
                    testDownloadListWithData(entityName, 67, 94);
                    ReportListDownloadExcelHearderLableMatch(entityName, 94,csAssert);
                    testDownloadListWithData(entityName, 67, 1008);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1008,csAssert);
                    testDownloadListWithData(entityName, 67, 51);
                    ReportListDownloadExcelHearderLableMatch(entityName, 51,csAssert);
                    testDownloadListWithData(entityName, 67, 438);
                    ReportListDownloadExcelHearderLableMatch(entityName, 438,csAssert);
                    testDownloadListWithData(entityName, 67, 81);
                    ReportListDownloadExcelHearderLableMatch(entityName, 81,csAssert);
                    testDownloadListWithData(entityName, 67, 90);
                    ReportListDownloadExcelHearderLableMatch(entityName, 90,csAssert);
                    testDownloadListWithData(entityName, 67, 201);
                    ReportListDownloadExcelHearderLableMatch(entityName, 201,csAssert);
                    testDownloadListWithData(entityName, 67, 78);
                    ReportListDownloadExcelHearderLableMatch(entityName, 78,csAssert);
                    testDownloadListWithData(entityName, 67, 93);
                    ReportListDownloadExcelHearderLableMatch(entityName, 93,csAssert);
                }else if(entityName.toLowerCase().contains("work order requests")) {
                    testDownloadListWithData(entityName, 80, 412);
                    ReportListDownloadExcelHearderLableMatch(entityName, 412,csAssert);
                    testDownloadListWithData(entityName, 80, 379);
                    ReportListDownloadExcelHearderLableMatch(entityName, 379,csAssert);
                    testDownloadListWithData(entityName, 80, 1006);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1006,csAssert);
                    testDownloadListWithData(entityName, 80, 275);
                    ReportListDownloadExcelHearderLableMatch(entityName, 275,csAssert);
                    testDownloadListWithData(entityName, 80, 96);
                    ReportListDownloadExcelHearderLableMatch(entityName, 96,csAssert);
                    testDownloadListWithData(entityName, 80, 202);
                    ReportListDownloadExcelHearderLableMatch(entityName, 202,csAssert);
                    testDownloadListWithData(entityName, 80, 203);
                    ReportListDownloadExcelHearderLableMatch(entityName, 203,csAssert);
                    testDownloadListWithData(entityName, 80, 84);
                    ReportListDownloadExcelHearderLableMatch(entityName, 84,csAssert);
                    testDownloadListWithData(entityName, 80, 85);
                    ReportListDownloadExcelHearderLableMatch(entityName, 85,csAssert);
                }else if(entityName.toLowerCase().contains("governance body")) {
                    testDownloadListWithData(entityName, 86, 261);
                    ReportListDownloadExcelHearderLableMatch(entityName, 261,csAssert);
                }else if(entityName.toLowerCase().contains("governance body meetings")) {
                    testDownloadListWithData(entityName, 87, 264);
                    ReportListDownloadExcelHearderLableMatch(entityName, 264,csAssert);
                }else if(entityName.toLowerCase().contains("contract draft request")) {
                    testDownloadListWithData(entityName, 160, 1011);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1011,csAssert);
                    testDownloadListWithData(entityName, 160, 446);
                    ReportListDownloadExcelHearderLableMatch(entityName, 446,csAssert);
                    testDownloadListWithData(entityName, 160, 324);
                    ReportListDownloadExcelHearderLableMatch(entityName, 324,csAssert);
                }else if(entityName.toLowerCase().contains("purchase orders")) {
                    testDownloadListWithData(entityName, 181, 386);
                    ReportListDownloadExcelHearderLableMatch(entityName, 386,csAssert);
                }else if(entityName.toLowerCase().contains("service data")) {
                    testDownloadListWithData(entityName, 64, 444);
                    ReportListDownloadExcelHearderLableMatch(entityName, 444,csAssert);
                    testDownloadListWithData(entityName, 64, 355);
                    ReportListDownloadExcelHearderLableMatch(entityName, 355,csAssert);
                    testDownloadListWithData(entityName, 64, 442);
                    ReportListDownloadExcelHearderLableMatch(entityName, 442,csAssert);
                }else if(entityName.toLowerCase().contains("invoice line item")) {
                    testDownloadListWithData(entityName, 165, 359);
                    ReportListDownloadExcelHearderLableMatch(entityName, 359,csAssert);
                }else if(entityName.toLowerCase().contains("consumptions")) {
                    testDownloadListWithData(entityName, 176, 402);
                    ReportListDownloadExcelHearderLableMatch(entityName, 402,csAssert);
                    testDownloadListWithData(entityName, 176, 401);
                    ReportListDownloadExcelHearderLableMatch(entityName, 401,csAssert);
                    testDownloadListWithData(entityName, 176, 385);
                    ReportListDownloadExcelHearderLableMatch(entityName, 385,csAssert);
                    testDownloadListWithData(entityName, 176, 1012);
                    ReportListDownloadExcelHearderLableMatch(entityName, 1012,csAssert);
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Field Labels on Report Page "+ReportID+" of " + entityName + ". " + e.getMessage());
            }
        }
    }
    private List<String> setHeadersInCSVFile() {
        List<String> headers = new ArrayList<String>();
        String allColumns[] = {"Index", "TestMethodName", "EntityName", "TestMethodResult", "Comments", "ErrorMessage"};
        for (String columnName : allColumns)
            headers.add(columnName);
        return headers;
    }

    private boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            return fileUtil.writeResponseIntoFile(response, outputFile);
        }
        return false;
    }

    private void downloadListDataForAllColumns(String entityName,Integer entityTypeId, Integer listId, CustomAssert csAssert){
        DownloadListWithData dlwdObj = new DownloadListWithData();
        Map<String, String> formParam = dlwdObj.getDownloadListWithDataPayload(entityTypeId);
        logger.info("formParam is : [{}]", formParam);
        logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
        HttpResponse response = dlwdObj.hitDownloadListWithData(formParam, listId);
        if (response.getStatusLine().toString().contains("200")) {
            Map<String, String> resultsMap = new HashMap<String, String>();
            resultsMap.put("Index", String.valueOf(++globalIndex));
            resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
            resultsMap.put("EntityName", entityName);
            resultsMap.put("TestMethodResult", "Pass");
            resultsMap.put("Comments", "NA");
            resultsMap.put("ErrorMessage", "NA");
            DumpResultsIntoCSV dumpResultsObj = new DumpResultsIntoCSV(outputFilePath, entityName + ".xlsx", setHeadersInCSVFile());
            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
            csAssert.assertTrue(dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ReportDataDownloadOutput", entityName, "AllColumn"),
                    "ListData Download failed for Entity " + entityName + ", List Id: " + 3 + " for All Columns.");
        } else {
            csAssert.assertTrue(false, "ListData Download API Response for Entity " + entityName + ", List Id: " + listId + " for All Columns is not 200");

            Map<String, String> resultsMap = new HashMap<String, String>();
            resultsMap.put("Index", String.valueOf(++globalIndex));
            resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
            resultsMap.put("EntityName", entityName);
            resultsMap.put("TestMethodResult", "Fail");
            resultsMap.put("Comments", "NA");
            resultsMap.put("ErrorMessage", "NA");
            DumpResultsIntoCSV dumpResultsObj = new DumpResultsIntoCSV(outputFilePath,entityName+".xlsx",setHeadersInCSVFile());
            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
        }
    }

    public void testDownloadListWithData(String entityName, Integer entityTypeId, Integer entityListId) {
        CustomAssert csAssert = new CustomAssert();
        logger.info("***************** Validating download List with data for entity {} ************", entityName);
        try {
            if (entityTypeId != 0) {
                ListRendererListData listObj = new ListRendererListData();
                listObj.hitListRendererListData(entityListId);
                String listRendererJsonStr = listObj.getListDataJsonStr();
                if (APIUtils.validJsonResponse(listRendererJsonStr, "[listRendererJsonStr response]")) {
                    String metaDataResponseStr = getMetaDataResponseStr(entityListId, entityName);
                    if (APIUtils.validJsonResponse(metaDataResponseStr, "defaultUserListMetaData Response For Entity : " + entityName)) {
                        Boolean isDownloadAvailable = (new JSONObject(metaDataResponseStr)).getBoolean("download");
                        if (isDownloadAvailable) {
                            JSONObject listRendererDataJson = new JSONObject(listRendererJsonStr);
                            if (listRendererDataJson.getInt("totalCount") == 0)
                                logger.warn("enityName = {} Listing Page doesn't have any data. Hence skipping generation of downloadListWithData file for this entity.", entityName);

                            else {
                                downloadListDataForAllColumns(entityName, entityTypeId,entityListId, csAssert);
                            }

                        } else {
                            logger.warn("download List is not available (since download = false) for entity {} . Hence skipping download data for this entity.", entityName);
                        }
                    } else {
                        csAssert.assertTrue(false, "Default User List Meta Data response is not valid json for entity = " + entityName);
                        logger.error("Default User List Meta Data response is not valid json for entity = " + entityName);
                    }

                }
            } else
                logger.warn("Entity Id not found for the entity {}", entityName);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception occurred in testDownloadListWithData. " + e.getMessage());
            logger.error("Exception occurred in testDownloadListWithData for the entity {}. {} ", entityName, e.getMessage());
        }
        csAssert.assertAll();
    }

    private String getMetaDataResponseStr(Integer listId, String entityName) {
        String responseStr = null;
        try {
            ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
            metaDataObj.hitListRendererDefaultUserListMetadata(listId);
            responseStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();
        } catch (Exception e) {
            logger.error("Exception occurred while hitting metaData API for entity {}", entityName);
        }
        return responseStr;
    }

    public void ReportListDownloadExcelHearderLableMatch (String entityName,int listId,CustomAssert csAssert){

        List<String> excelHeaders = XLSUtils.getExcelDataOfOneRow(outputFilePath + "/ReportDataDownloadOutput/" + entityName ,"AllColumn.xlsx","Data"+expectedPostFix,3);
        for(String Headers:excelHeaders){
            if (Headers.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                csAssert.assertTrue(false, "Field Label: [" + Headers.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under download excel of "+entityName + " and list id is: "+listId);
            } else {
                csAssert.assertTrue(true, "Field Label: [" + Headers.toLowerCase() + "] does not contain: [" + expectedPostFix.toLowerCase() + "] under download excel of "+entityName  + " and list id is: "+ listId);
            }
        }
    }
}