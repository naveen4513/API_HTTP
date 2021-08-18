package com.sirionlabs.test.reports;

import com.sirionlabs.api.AllCommonAPI.APIExecutorCommon;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class TestLinkedReport {
    private final static Logger logger = LoggerFactory.getLogger(TestLinkedReport.class);
    private String payload;
    private int entityId;
    private String sourceId;
    private Integer entityTypeId;

    @BeforeClass
    public void createContract() {
        logger.info("Create Contract ");
        String entityResponse = Contract.createContract("fixed fee flow 1", true);
        if (ParseJsonResponse.validJsonResponse(entityResponse)) {
            entityId = CreateEntity.getNewEntityId(entityResponse);
            logger.info("contract id {}", entityId);
            sourceId = getShortId(entityResponse, "");
            logger.info("source id {}", sourceId);
        } else logger.error("entity not created");
        entityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
        payload = "{\"entityId\":" + entityId + ",\"entityTypeId\":" + entityTypeId + ",\"linkEntities\":[{\"entityId\":32740,\"entityTypeId\":61},{\"entityId\":5413,\"entityTypeId\":160}]}";
    }

    @DataProvider
    public Object[][] allDataForTestCase() {
        List<Object[]> allTestData = new ArrayList<>();
        String data = "CDR05158:API Automation CDR Creation Flow 1:Linked\n" +
                "CO32001:Test Contract Creation:Linked";
        String reportName = "Contract - Pipeline Report - New";
        int listId = 270;
        allTestData.add(new Object[]{data, reportName, listId});
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "allDataForTestCase")
    public void testLinkedEntities(String data, String reportName, int listId) {

        logger.info("link contract and CDR With newly created Contract");
        CustomAssert customAssert = new CustomAssert();
        APIExecutorCommon apiExecutorCommon = new APIExecutorCommon();
        try {
            logger.info("hit /linkentity/link API");

            APIResponse apiResponse = apiExecutorCommon.setApiPath("/linkentity/link")
                    .setHeaders("Accept", "application/json, text/plain, */*")
                    .setHeaders("Content-Type", "application/json;charset=UTF-8")
                    .setPayload(payload)
                    .setMethodType("post")
                    .getResponse();
            String linkEntityAPIResponseBody = apiResponse.getResponseBody();
            if (ParseJsonResponse.validJsonResponse(linkEntityAPIResponseBody)) {
                JSONObject jsonObject = new JSONObject(linkEntityAPIResponseBody);
                String output = jsonObject.get("data").toString();
                String error = jsonObject.get("error").toString();
                if (!output.equalsIgnoreCase("null") &&!error.equalsIgnoreCase("null")) {
                    customAssert.assertTrue(false, "Entity Not Linked Successfully with Contract and error message { " + error + " }");
                }
                logger.error("Download report with selected date {}", reportName);
                DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
                logger.info("excel file Download for listing");
                HashMap<String, String> formParam = new HashMap<>();
                formParam.put("jsonData", "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"111\":{\"filterId\":\"111\",\"filterName\":\"createdDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null,\"start\":\"" + getYesterdayDate() + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}}}");
                formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam, listId);
                if (response.getStatusLine().getStatusCode() == 200)
                    excelDownload(response, reportName, customAssert, data);
                else
                    customAssert.assertTrue(false, "API Given Invalid response code { " + response.getStatusLine().getStatusCode() + "}");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying link entity test case");
            customAssert.assertTrue(false, e.getMessage());
        } finally {
            logger.error("delete contract {}", entityId);
            ShowHelper.deleteEntity("contracts", entityTypeId, entityId);
        }
        customAssert.assertAll();
    }

    private void excelDownload(HttpResponse response, String entityName, CustomAssert customAssert, String data) {
        String outputFilePath = "src/test/output";
        String outputFileName = "Contract - Pipeline Report - New.xlsx";
        try {
            FileUtils fileUtil = new FileUtils();
            Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);
            if (!fileDownloaded) throw new SkipException("Couldn't Download Data for Entity [" + entityName + "]");
            Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, outputFileName, "Data");
            List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, outputFileName, "Data", 4,
                    noOfRows.intValue() - 5);
            List<String> allHeaders = XLSUtils.getOffSetHeaders(outputFilePath, outputFileName, "data");
            int linkEntityColumnId = allHeaders.indexOf("LINKED ENTITIES");
            boolean contractFoundInReport = true;
            for (List<String> allData : allRecordsData) {
                if (allData.contains(sourceId)) {
                    String linkedEntity = allData.get(linkEntityColumnId);
                    contractFoundInReport = false;
                    if (!linkedEntity.equalsIgnoreCase(data)) {
                        logger.info("Linked Entity {}", data);
                        customAssert.assertTrue(false, "Linked Entity Not Found In Report { " + entityName + "}");
                    }
                    break;
                }
            }
            if (contractFoundInReport) {
                customAssert.assertTrue(false, "Newly created Contract not found in report {" + entityName + "}");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying link entity test case");
            customAssert.assertTrue(false, e.getMessage());
        } finally {
            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
        }
        customAssert.assertAll();
    }

    private String getShortId(String createJsonStr, String entityName) {
        String newEntityId = "";
        try {
            if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
                JSONObject jsonObj = new JSONObject(createJsonStr);
                String notificationStr = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");
                String[] temp = notificationStr.trim().split(Pattern.quote("show/"));
                if (temp.length > 1) {
                    String temp2 = temp[1];
                    String[] temp3 = temp2.trim().split(Pattern.quote("\""));
                    if (temp3.length > 1) {
                        String temp4 = temp3[3];
                        String[] temp5 = temp4.trim().split(Pattern.quote("/"));
                        if (temp5.length > 1)
                            newEntityId = temp5[0].substring(1, temp5[0].length() - 1);
                    }
                }
            } else {
                logger.error("New Entity {} not created. ", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Entity Id of Newly Created Entity {}. {}", entityName, e.getStackTrace());
        }
        return newEntityId;
    }


    private String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        return dateFormat.format(cal.getTime());

    }
}
