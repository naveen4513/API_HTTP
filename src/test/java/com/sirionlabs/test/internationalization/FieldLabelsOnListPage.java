package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldLabelsOnListPage extends TestDisputeInternationalization {
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnListPage.class);
    private static int globalIndex = 0;
    void verifyFieldLabelsOnListPage(String entityName, CustomAssert csAssert) {
        try {
            logger.info("Validating Field Labels on List Page of " + entityName);
            DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
            String listDataResponse = defaultUserListMetadataHelper.getDefaultUserListMetadataResponse(entityName);
            List<String> listDataColumnResponse = defaultUserListMetadataHelper.getAllColumnNames(listDataResponse);
            List<String> listDataFiltersResponse = defaultUserListMetadataHelper.getAllFilterMetadataNames(listDataResponse);
            for ( String columnName : listDataColumnResponse ) {
                if (columnName.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                    csAssert.assertTrue(false, "Field Label: [" + columnName.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under listing columns of "+entityName);
                } else {
                    csAssert.assertTrue(true, "Field Label: [" + columnName + "] does not contain: [" + expectedPostFix + "] under listing columns of "+entityName);
                }
            }
            for ( String filterName : listDataFiltersResponse ) {
                if (filterName.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                    csAssert.assertTrue(false, "Field Label: [" + filterName.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under listing filters of "+entityName+" lisitng page");
                } else {
                    csAssert.assertTrue(true, "Field Label: [" + filterName.toLowerCase() + "] does not contain: [" + expectedPostFix.toLowerCase() + "] under listing filters of "+entityName+" listing page");
                }
            }

            if(entityName.toLowerCase().contains("suppliers")) {
                testDownloadListWithData(entityName, 1, 2);
                ListDownloadExcelHearderLableMatch(entityName, 2,csAssert);
            }else if(entityName.toLowerCase().contains("vendors")) {
                testDownloadListWithData(entityName, 3, 251);
                ListDownloadExcelHearderLableMatch(entityName, 251,csAssert);
            }else if(entityName.toLowerCase().contains("obligations")) {
                testDownloadListWithData(entityName, 12, 4);
                ListDownloadExcelHearderLableMatch(entityName, 4,csAssert);
            }else if(entityName.toLowerCase().contains("child obligations")) {
                testDownloadListWithData(entityName, 13, 5);
                ListDownloadExcelHearderLableMatch(entityName, 5,csAssert);
            } else if(entityName.toLowerCase().contains("service levels")) {
                testDownloadListWithData(entityName, 14, 6);
                ListDownloadExcelHearderLableMatch(entityName, 6,csAssert);
            } else if(entityName.toLowerCase().contains("child service levels")) {
                testDownloadListWithData(entityName, 15, 265);
                ListDownloadExcelHearderLableMatch(entityName, 265,csAssert);
            } else if(entityName.toLowerCase().contains("interpretations")) {
                testDownloadListWithData(entityName, 16, 11);
                ListDownloadExcelHearderLableMatch(entityName, 11,csAssert);
            } else if(entityName.toLowerCase().contains("issues")) {
                testDownloadListWithData(entityName, 17, 8);
                ListDownloadExcelHearderLableMatch(entityName, 8,csAssert);
            } else if(entityName.toLowerCase().contains("actions")) {
                testDownloadListWithData(entityName, 18, 9);
                ListDownloadExcelHearderLableMatch(entityName, 9,csAssert);
            }else if(entityName.toLowerCase().contains("disputes")) {
                testDownloadListWithData(entityName, 28, 286);
                ListDownloadExcelHearderLableMatch(entityName, 286,csAssert);
            }else if(entityName.toLowerCase().contains("contracts")) {
                testDownloadListWithData(entityName, 61, 2);
                ListDownloadExcelHearderLableMatch(entityName, 2, csAssert);
                testDownloadListWithData(entityName, 61, 354);
                ListDownloadExcelHearderLableMatch(entityName, 354,csAssert);
            }else if(entityName.toLowerCase().contains("change requests")) {
                testDownloadListWithData(entityName, 63, 1);
                ListDownloadExcelHearderLableMatch(entityName, 1, csAssert);
            }else if(entityName.toLowerCase().contains("invoices")) {
                testDownloadListWithData(entityName, 67, 10);
                ListDownloadExcelHearderLableMatch(entityName, 10,csAssert);
            }else if(entityName.toLowerCase().contains("work order requests")) {
                testDownloadListWithData(entityName, 80, 12);
                ListDownloadExcelHearderLableMatch(entityName, 12,csAssert);
            }else if(entityName.toLowerCase().contains("governance body")) {
                testDownloadListWithData(entityName, 86, 211);
                ListDownloadExcelHearderLableMatch(entityName, 211,csAssert);
            }else if(entityName.toLowerCase().contains("governance body meetings")) {
                testDownloadListWithData(entityName, 87, 212);
                ListDownloadExcelHearderLableMatch(entityName, 212,csAssert);
            }else if(entityName.toLowerCase().contains("contract draft request")) {
                testDownloadListWithData(entityName, 160, 279);
                ListDownloadExcelHearderLableMatch(entityName, 279,csAssert);
            }else if(entityName.toLowerCase().contains("purchase orders")) {
                testDownloadListWithData(entityName, 181, 318);
                ListDownloadExcelHearderLableMatch(entityName, 318,csAssert);
            }else if(entityName.toLowerCase().contains("service data")) {
                testDownloadListWithData(entityName, 64, 447);
                ListDownloadExcelHearderLableMatch(entityName, 447,csAssert);
                testDownloadListWithData(entityName, 64, 352);
                ListDownloadExcelHearderLableMatch(entityName, 352,csAssert);
            }else if(entityName.toLowerCase().contains("invoice line item")) {
                testDownloadListWithData(entityName, 165, 358);
                ListDownloadExcelHearderLableMatch(entityName, 358,csAssert);
            }else if(entityName.toLowerCase().contains("clauses")) {
                testDownloadListWithData(entityName, 138, 271);
                ListDownloadExcelHearderLableMatch(entityName, 271,csAssert);
            }else if(entityName.toLowerCase().contains("contract templates")) {
                testDownloadListWithData(entityName, 140, 273);
                ListDownloadExcelHearderLableMatch(entityName, 273,csAssert);
            }else if(entityName.toLowerCase().contains("consumptions")) {
                testDownloadListWithData(entityName, 176, 375);
                ListDownloadExcelHearderLableMatch(entityName, 375,csAssert);
            }else if(entityName.toLowerCase().contains("contract temaplate structure")) {
                testDownloadListWithData(entityName,139,272);
                ListDownloadExcelHearderLableMatch(entityName,272,csAssert);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Field Labels on New Page of " + entityName + ". " + e.getMessage());
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
            csAssert.assertTrue(dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "AllColumn"),
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

    public void ListDownloadExcelHearderLableMatch (String entityName,int listId,CustomAssert csAssert){

        List<String> excelHeaders = XLSUtils.getExcelDataOfOneRow(outputFilePath + "/ListDataDownloadOutput/" + entityName ,"AllColumn.xlsx","Data"+expectedPostFix,4);
        for(String Headers:excelHeaders){
            if (Headers.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                csAssert.assertTrue(false, "Field Label: [" + Headers.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under download excel of "+entityName + " and list id is: "+listId);
            } else {
                csAssert.assertTrue(true, "Field Label: [" + Headers.toLowerCase() + "] does not contain: [" + expectedPostFix.toLowerCase() + "] under download excel of "+entityName  + " and list id is: "+ listId);
            }
        }
    }
}