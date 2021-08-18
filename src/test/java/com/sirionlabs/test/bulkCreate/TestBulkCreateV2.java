package com.sirionlabs.test.bulkCreate;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class TestBulkCreateV2 {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkCreateV2.class);

    private String configFilePath;
    private String configFileName;

    private int invoiceEntityTypeId = 67;
    private int lineItemEntityTypeId = 165;
    private String dateFormat = "dd-MM-yyyy";

    String invoiceSheetName = "Invoice";
    String lineItemSheetName = "Invoice Line Item";
    int clientId = 1007;
    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestBulkCreateFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestBulkCreateFileName");
        clientId = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id"));

    }

//    C152084
    @Test(dataProvider = "bulkCreatePosFlows",enabled = true)
    public void Test_BulkCreateInvoiceV2(String scenario){

        CustomAssert customAssert = new CustomAssert();

        Download download = new Download();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        String bulkCreateFilePath = "src\\test\\resources\\TestConfig\\BulkCreate\\ExcelSheets";
        String bulkCreateFileName = "Invoice_Bulk_Create.xlsm";

        try {

            int rowNum = 6;
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "template id"));
            int parentEntityTypeId = 61;
            int parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contract id"));

            if(scenario.equals("contract scenario")){
                parentEntityTypeId = 61;
                parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contract id"));

            }else if(scenario.equals("supplier scenario")){
                parentEntityTypeId = 1;
                parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplier id"));

            }

            download.hitDownload(bulkCreateFilePath, bulkCreateFileName, templateId, parentEntityTypeId, parentId);

            List<String> columnNumbersInvoice = XLSUtils.getExcelDataOfOneRow(bulkCreateFilePath,bulkCreateFileName,invoiceSheetName,2);
            List<String> columnNumbersLineItem = XLSUtils.getExcelDataOfOneRow(bulkCreateFilePath,bulkCreateFileName,lineItemSheetName,2);

            Map<String,String> columnValuesInvoice = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath,configFileName,"column values invoice bulk create");
            Map<String,String> columnValuesLineItem = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath,configFileName,"column values line item bulk create");
            Map<String,String> dynamicValues = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath,configFileName,"dynamic field values");

            String startDate= ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"startdate");
            String endDate= ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"enddate");

            for(int i =0;i<columnNumbersInvoice.size();i++) {

                if (columnValuesInvoice.containsKey(columnNumbersInvoice.get(i)) || dynamicValues.containsKey(columnNumbersInvoice.get(i))) {

                    //If column types are of date
                    if(columnNumbersInvoice.get(i).equals("611") || columnNumbersInvoice.get(i).equals("617") ||
                            columnNumbersInvoice.get(i).equals("612") || columnNumbersInvoice.get(i).equals("613")
                            || columnNumbersInvoice.get(i).equals("103504")){
                        if(columnValuesInvoice.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, dateFormat, columnValuesInvoice.get(columnNumbersInvoice.get(i)));
                        }else if(dynamicValues.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, dateFormat,dynamicValues.get(columnNumbersInvoice.get(i)));
                        }

                    }else if(columnNumbersInvoice.get(i).equals("100000001") || columnNumbersInvoice.get(i).equals("103487") ||
                                columnNumbersInvoice.get(i).equals("103502")){

                        if(columnValuesInvoice.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, Integer.parseInt(columnValuesInvoice.get(columnNumbersInvoice.get(i))));
                        }else if(dynamicValues.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, Integer.parseInt(dynamicValues.get(columnNumbersInvoice.get(i))));
                        }

                    } else {
                        if(columnValuesInvoice.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, columnValuesInvoice.get(columnNumbersInvoice.get(i)));
                        } else if(dynamicValues.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, dynamicValues.get(columnNumbersInvoice.get(i)));
                        }

                    }
                }
            }
            for(int i =0;i<columnNumbersLineItem.size();i++) {
                if(columnValuesLineItem.containsKey(columnNumbersLineItem.get(i))|| dynamicValues.containsKey(dynamicValues.get(i))){
                    if(columnNumbersLineItem.get(i).equals("11067") || columnNumbersLineItem.get(i).equals("11068")){
                        if(columnValuesLineItem.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, dateFormat, columnValuesLineItem.get(columnNumbersLineItem.get(i)));
                        }else if(dynamicValues.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, dateFormat,dynamicValues.get(columnNumbersLineItem.get(i)));
                        }
                    }else if(columnNumbersLineItem.get(i).equals("100000001") || columnNumbersLineItem.get(i).equals("100000003")
                            || columnNumbersLineItem.get(i).equals("103487")){
                        if(columnValuesLineItem.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, Integer.parseInt(columnValuesLineItem.get(columnNumbersLineItem.get(i))));
                        }else if(dynamicValues.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, Integer.parseInt(dynamicValues.get(columnNumbersLineItem.get(i))));
                        }
                    } else {
                        if(columnValuesLineItem.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, columnValuesLineItem.get(columnNumbersLineItem.get(i)));
                        }else if(dynamicValues.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i,dynamicValues.get(columnNumbersLineItem.get(i)));
                        }
                    }
                }
            }


            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityTypeId", Integer.toString(parentEntityTypeId));
            payloadMap.put("parentEntityId", Integer.toString(parentId));
            payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            payloadMap.put("upload", "submit");

            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            UploadBulkData uploadObj = new UploadBulkData();
            uploadObj.hitUploadBulkData(invoiceEntityTypeId, templateId, bulkCreateFilePath, bulkCreateFileName, payloadMap);
            String uploadResponse = uploadObj.getUploadBulkDataJsonStr();

            if (uploadResponse.contains("200")) {

                fetchObj.hitFetch();

                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
//                Thread.sleep(300000);
                int requestId = UserTasksHelper.getRequestIdFromTaskId(newTaskId);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(300000L, 20000L, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("false")) {

                    customAssert.assertTrue(false, "Bulk Update Scheduler Job Failed whereas it was supposed to pass for Flow");
                }else {
                    if (requestId != -1) {
                        SCPUtils scpUtils = new SCPUtils();

                        String bulkCrRespFilePath = "src\\test\\output\\BulkResponse\\Bulk Create";
                        String bulkCrRespFileName = "Bulk_Create_Response.xlsm";

                        Boolean downloadStatus = scpUtils.downloadExcelFile(String.valueOf(requestId), bulkCrRespFileName, bulkCrRespFilePath);

                        if (downloadStatus) {
                            ArrayList<String> newlyCreatedInv = validateBulkCreateResponseExcel(bulkCrRespFilePath, bulkCrRespFileName, invoiceSheetName, customAssert);

                            ArrayList<String> newlyCreatedLi = validateBulkCreateResponseExcel(bulkCrRespFilePath, bulkCrRespFileName, lineItemSheetName, customAssert);


                            for (int i = 0; i < newlyCreatedInv.size(); i++) {

                                int invoiceShortCodeId = Integer.parseInt(newlyCreatedInv.get(0).split("INV")[1]);
                                try {
                                    int invoiceId = Integer.parseInt(postgreSQLJDBC.doSelect("select id from base_invoice where client_entity_seq_id = " + invoiceShortCodeId + " and client_id = " + clientId).get(0).get(0));

                                    validateShowPageValues(invoiceEntityTypeId, invoiceId, columnValuesInvoice, customAssert);

                                    validateCustomFieldShowPageValues(invoiceEntityTypeId,invoiceId,dynamicValues,customAssert);

                                    validateLocalListing(invoiceId,"invoices",parentEntityTypeId,parentId,invoiceEntityTypeId,customAssert);

                                } catch (Exception e) {
                                    customAssert.assertTrue(false, "Exception while fetching show page id from DB");
                                }
                            }

                            for (int i = 0; i < newlyCreatedLi.size(); i++) {

                                int lineItemShortCodeId = Integer.parseInt(newlyCreatedLi.get(0).split("LI")[1]);

                                int lineItemId = Integer.parseInt(postgreSQLJDBC.doSelect("select id from invoice_line_item where client_entity_seq_id = " + lineItemShortCodeId + " and client_id = " + clientId).get(0).get(0));

                                validateShowPageValues(lineItemEntityTypeId, lineItemId, columnValuesLineItem, customAssert);

                                validateLocalListing(lineItemId,"invoice line item",parentEntityTypeId,parentId,lineItemEntityTypeId,customAssert);

                            }
                        } else {
                            customAssert.assertTrue(false, "Bulk Create Response File download failed for request Id " + requestId);
                        }
                    } else {
                        customAssert.assertTrue(false, "Bulk Request Id = -1 after Bulk create upload ");
                    }


                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }

    @DataProvider(name = "bulkCreateNegFlows", parallel = false)
    public Object[][] bulkCreateNegFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"bulk create neg flows to test").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "bulkCreatePosFlows", parallel = false)
    public Object[][] bulkCreatePosFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"bulk create pos flows to test").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "bulkCreateNegFlows",enabled = false)
    public void Test_BulkCreateInvoiceV2_NegativeScenarios(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        Download download = new Download();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        String bulkCreateFilePath = "src\\test\\resources\\TestConfig\\BulkCreate\\ExcelSheets";
        String bulkCreateFileName = "Invoice_Bulk_Create.xlsm";

        try {

            int rowNum = 6;
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "template id"));
            int parentEntityTypeId = 61;
            int parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contract id"));

            download.hitDownload(bulkCreateFilePath, bulkCreateFileName, templateId, parentEntityTypeId, parentId);

            List<String> columnNumbersInvoice = XLSUtils.getExcelDataOfOneRow(bulkCreateFilePath,bulkCreateFileName,invoiceSheetName,2);
            List<String> columnNumbersLineItem = XLSUtils.getExcelDataOfOneRow(bulkCreateFilePath,bulkCreateFileName,lineItemSheetName,2);

            Map<String,String> columnValuesInvoice;
            if(flowToTest.contains("static")){
                columnValuesInvoice = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath,configFileName,"static fields neg sce 1");
            }else {
                columnValuesInvoice = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath,configFileName,"column values invoice bulk create");
            }

            Map<String,String> dynamicValues;

            if(flowToTest.contains("dyn")) {
                dynamicValues = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, flowToTest);
            }else {
                dynamicValues = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "dynamic field values");
            }

            Map<String,String> columnValuesLineItem = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath,configFileName,"column values line item bulk create");

            for(int i =0;i<columnNumbersInvoice.size();i++) {

                if (columnValuesInvoice.containsKey(columnNumbersInvoice.get(i)) || dynamicValues.containsKey(columnNumbersInvoice.get(i))) {

                    //If column types are of date
                    if(columnNumbersInvoice.get(i).equals("611") || columnNumbersInvoice.get(i).equals("617") ||
                            columnNumbersInvoice.get(i).equals("612") || columnNumbersInvoice.get(i).equals("613")
                            || columnNumbersInvoice.get(i).equals("103504")){
                        if(columnValuesInvoice.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, dateFormat, columnValuesInvoice.get(columnNumbersInvoice.get(i)));
                        }else if(dynamicValues.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, dateFormat,dynamicValues.get(columnNumbersInvoice.get(i)));
                        }

                    }else if(columnNumbersInvoice.get(i).equals("100000001") || columnNumbersInvoice.get(i).equals("103487") ||
                            columnNumbersInvoice.get(i).equals("103502")){

                        if(columnValuesInvoice.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, Integer.parseInt(columnValuesInvoice.get(columnNumbersInvoice.get(i))));
                        }else if(dynamicValues.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, Integer.parseInt(dynamicValues.get(columnNumbersInvoice.get(i))));
                        }

                    } else {
                        if(columnValuesInvoice.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, columnValuesInvoice.get(columnNumbersInvoice.get(i)));
                        } else if(dynamicValues.get(columnNumbersInvoice.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, invoiceSheetName, rowNum, i, dynamicValues.get(columnNumbersInvoice.get(i)));
                        }

                    }
                }
            }
            for(int i =0;i<columnNumbersLineItem.size();i++) {
                if(columnValuesLineItem.containsKey(columnNumbersLineItem.get(i))|| dynamicValues.containsKey(dynamicValues.get(i))){
                    if(columnNumbersLineItem.get(i).equals("11067") || columnNumbersLineItem.get(i).equals("11068")){
                        if(columnValuesLineItem.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, dateFormat, columnValuesLineItem.get(columnNumbersLineItem.get(i)));
                        }else if(dynamicValues.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValueDate(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, dateFormat,dynamicValues.get(columnNumbersLineItem.get(i)));
                        }
                    }else if(columnNumbersLineItem.get(i).equals("100000001") || columnNumbersLineItem.get(i).equals("100000003")
                            || columnNumbersLineItem.get(i).equals("103487")){
                        if(columnValuesLineItem.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, Integer.parseInt(columnValuesLineItem.get(columnNumbersLineItem.get(i))));
                        }else if(dynamicValues.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, Integer.parseInt(dynamicValues.get(columnNumbersLineItem.get(i))));
                        }
                    } else {
                        if(columnValuesLineItem.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i, columnValuesLineItem.get(columnNumbersLineItem.get(i)));
                        }else if(dynamicValues.get(columnNumbersLineItem.get(i)) != null) {
                            XLSUtils.updateColumnValue(bulkCreateFilePath, bulkCreateFileName, lineItemSheetName, rowNum, i,dynamicValues.get(columnNumbersLineItem.get(i)));
                        }
                    }
                }
            }


            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityTypeId", Integer.toString(parentEntityTypeId));
            payloadMap.put("parentEntityId", Integer.toString(parentId));
            payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            payloadMap.put("upload", "submit");

            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            UploadBulkData uploadObj = new UploadBulkData();
            uploadObj.hitUploadBulkData(invoiceEntityTypeId, templateId, bulkCreateFilePath, bulkCreateFileName, payloadMap);
            String uploadResponse = uploadObj.getUploadBulkDataJsonStr();

            if (uploadResponse.contains("200")) {

                fetchObj.hitFetch();

                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                int requestId = UserTasksHelper.getRequestIdFromTaskId(newTaskId);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(300000L, 20000L, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further");
                }

                if (!schedulerJob.get("jobPassed").equalsIgnoreCase("false")) {

                    customAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to Failed for Flow");
                }else {
                    if (requestId != -1) {
                        SCPUtils scpUtils = new SCPUtils();

                        String bulkCrRespFilePath = "src\\test\\output\\BulkResponse\\Bulk Create";
                        String bulkCrRespFileName = "Bulk_Create_Response.xlsm";

                        Boolean downloadStatus = scpUtils.downloadExcelFile(String.valueOf(requestId), bulkCrRespFileName, bulkCrRespFilePath);

                        if (downloadStatus) {

                            List<String> expectedExcelResponseList = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "expected response neg scenarios", flowToTest).split("&&"));

                            ArrayList<String> excelResponseList = getBulkCrRespExcelFailMsg(bulkCrRespFilePath, bulkCrRespFileName, invoiceSheetName, customAssert);

                            List<String> actualExcelResponseList = new ArrayList<>();

                            for (int i = 0; i < excelResponseList.size(); i++) {

                                try {
                                    String[] actualResponseExcelArray = excelResponseList.get(i).split("Failure :")[1].split(" , ");

                                    for (int j = 0; j < actualResponseExcelArray.length; j++) {
                                        actualExcelResponseList.add(actualResponseExcelArray[j].trim());
                                    }

                                } catch (Exception e) {
                                    logger.error("Exception while Parsing Failure messages in excel " + e.getStackTrace());
                                }
                            }

                            for (String expRespMsg : expectedExcelResponseList) {

                                if (!actualExcelResponseList.contains(expRespMsg)) {
                                    customAssert.assertTrue(false, "Expected Response Msg " + expRespMsg + " not found in the Bulk Create Response Excel");
                                }

                            }

                        } else {
                            customAssert.assertTrue(false, "Bulk Create Response File download failed for request Id " + requestId);
                        }
                    } else {
                        customAssert.assertTrue(false, "Bulk Request Id = -1 after Bulk create upload ");
                    }
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    private HashMap<String,String> listColumnMapInvoice(String startDate,String endDate,String createdDate){

        HashMap<String,String> listColumnMap = new HashMap<>();
        String payload = "{\"filterMap\": {\"entityTypeId\": 67,\"offset\": 0,\"size\": 20,\"orderByColumnName\": \"id\",\"orderDirection\": \"desc nulls last\",\n" +
                "    \"filterJson\": {\n" + "\t" +
                "\"111\": {\"filterId\": \"111\",\"filterName\": \"createdDate\",\"start\": \"" + createdDate + "\",\"end\": \"" + createdDate + "\",\"multiselectValues\": {\"SELECTEDDATA\": [{\"id\": \"1\",\"name\": \"Date\"}]}},\n" +
                "\"157\": {\"filterId\": \"157\",\"filterName\": \"invoicePeriodStartDate\",\"start\": \"" + startDate + "\",\"end\": \"" + startDate + "\",\"multiselectValues\": {\"SELECTEDDATA\": [{\"id\": \"1\",\"name\": \"Date\"}]}},\n" +
                "\"158\": {\"filterId\": \"158\",\"filterName\": \"invoicePeriodEndDate\",\"start\": \"" + endDate + "\",\"end\": \"" + endDate + "\",\"multiselectValues\": {\"SELECTEDDATA\": [{\"id\": \"1\",\"name\": \"Date\"}]}}\n" +
                "}},\"selectedColumns\": []}";

        try{

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(10,payload);

            String listResponse = listRendererListData.getListDataJsonStr();

            JSONObject listResponseJson = new JSONObject(listResponse);
            JSONObject firstRowJson = listResponseJson.getJSONArray("data").getJSONObject(0);
            Iterator<String> keys = firstRowJson.keys();
            String key;
            String columnName;
            String columnValue;
            while (keys.hasNext()){
                key = keys.next();

                columnName = firstRowJson.getJSONObject(key).get("columnName").toString();
                columnValue = firstRowJson.getJSONObject(key).get("value").toString();

                listColumnMap.put(columnName,columnValue);
            }

        }catch (Exception e){

        }

        return listColumnMap;
    }

//    C13785 C13786
    public ArrayList<String> validateBulkCreateResponseExcel(String bulkCrRespFilePath,String bulkCrRespFileName,
                                                             String sheetName,CustomAssert customAssert){

        ArrayList<String> newlyCreatedList = new ArrayList<>();
            try{
            List<String> columnNames = XLSUtils.getExcelDataOfOneRow(bulkCrRespFilePath,bulkCrRespFileName,sheetName,1);
            int creationStatusColumnNumInv = -1;
            if(columnNames.contains("Creation Status")){

                for(int i =0;i<columnNames.size();i++){
                    if(columnNames.get(i).equals("Creation Status")){
                        creationStatusColumnNumInv = i;
                        break;
                    }
                }
                int numOFRows = XLSUtils.getNoOfRows(bulkCrRespFilePath,bulkCrRespFileName,sheetName).intValue();

                List<String> creationStatusList  = XLSUtils.getOneColumnDataFromMultipleRows(bulkCrRespFilePath,bulkCrRespFileName,sheetName,creationStatusColumnNumInv,6,numOFRows);
                String creationStatus;
                for(int i =0;i<creationStatusList.size();i++){
                    creationStatus = creationStatusList.get(i);

                    if(creationStatus.contains("Fail")){
                        customAssert.assertTrue(false,"Bulk Creation On Invoice Failed ");
                        customAssert.assertTrue(false,"For Row Number " + i + " Failure Response In Excel File in the sheet " + sheetName + creationStatus);
                    }else if(creationStatus.contains("Success")){
                        String invId = creationStatus.split("Success :")[1];

                        newlyCreatedList.add(invId);

                    }else{
                        customAssert.assertTrue(false,"For Row Number " + i + " No creation status exists in bulk create Response Excel File in the sheet " + sheetName + creationStatus);
                    }
                }

            }else {
                customAssert.assertTrue(false,"In the Bulk Create Response Excel Creation Status column does not exists in the sheet " + sheetName + " ");
            }}catch (Exception e){customAssert.assertTrue(false,"Exception while fetching bulk create response from excel sheet " + sheetName);}

        return newlyCreatedList;
    }

    public ArrayList<String> getBulkCrRespExcelFailMsg(String bulkCrRespFilePath,String bulkCrRespFileName,
                                                             String sheetName,CustomAssert customAssert){

        ArrayList<String> creationRespList = new ArrayList<>();
        try {
            List<String> columnNames = XLSUtils.getExcelDataOfOneRow(bulkCrRespFilePath, bulkCrRespFileName, sheetName, 1);
            int creationStatusColumnNumInv = -1;
            if (columnNames.contains("Creation Status")) {

                for (int i = 0; i < columnNames.size(); i++) {
                    if (columnNames.get(i).equals("Creation Status")) {
                        creationStatusColumnNumInv = i;
                        break;
                    }
                }
                int numOFRows = XLSUtils.getNoOfRows(bulkCrRespFilePath, bulkCrRespFileName, sheetName).intValue();

                List<String> creationStatusList = XLSUtils.getOneColumnDataFromMultipleRows(bulkCrRespFilePath, bulkCrRespFileName, sheetName, creationStatusColumnNumInv, 6, numOFRows);
                String creationStatus;
                for (int i = 0; i < creationStatusList.size(); i++) {
                    creationStatus = creationStatusList.get(i);

                    if (creationStatus.contains("Fail")) {
                        creationRespList.add(creationStatus);
                    } else {
                        customAssert.assertTrue(false, "For Row Number " + i + " No creation Failure status exists in bulk create Response Excel File in the sheet " + sheetName + creationStatus);
                    }
                }

            } else {
                customAssert.assertTrue(false, "In the Bulk Create Response Excel Creation Status column does not exists in the sheet " + sheetName + " ");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting Bulk Create Response Excel Failure Messages");
        }
        return creationRespList;
    }

    private void validateShowPageValues(int entityTypeId,int entityId,Map<String,String> expectedColValues,CustomAssert customAssert){

        Show show = new Show();

        try {
            show.hitShowVersion2(entityTypeId, entityId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);
            JSONObject dataJson = showResponseJson.getJSONObject("body").getJSONObject("data");

            HashMap<Integer, String> fieldIdNameMap = ShowHelper.getFieldIdNameMap(entityTypeId, entityId, customAssert);

            for (Map.Entry<String, String> entry : expectedColValues.entrySet()) {

                int columnId = Integer.parseInt(entry.getKey());
                String expectedColumnValue = entry.getValue();

                if (columnId == 100000001 || columnId == 100000002) {
                    continue;
                } else {
                    if (fieldIdNameMap.containsKey(columnId)) {

                        String fieldName = fieldIdNameMap.get(columnId);
                        if (!fieldName.contains("dyn")) {
                            String actualShowPageValue = "";
                            try {
                                actualShowPageValue = dataJson.getJSONObject(fieldName).getJSONObject("values").get("name").toString();

                            } catch (Exception e1) {
                                try {
                                    actualShowPageValue = dataJson.getJSONObject(fieldName).get("displayValues").toString();
                                } catch (Exception e2) {
                                    try {
                                        actualShowPageValue = dataJson.getJSONObject(fieldName).get("values").toString();
                                    } catch (Exception e3) {
                                        logger.error("Exception while fetching value from show page for the field " + fieldName);
                                    }
                                }
                            }
                            actualShowPageValue = actualShowPageValue.replace(",","");
                            if (!(actualShowPageValue.contains(expectedColumnValue) || expectedColumnValue.contains(actualShowPageValue))) {
                                customAssert.assertTrue(false, "After Bulk Create Show Page Field " + fieldName + " not matched with expected Value for entity type id " + entityTypeId + " and entity id " + entityId);
                            }
                        } else {
                            String actualShowPageValue = dataJson.getJSONObject("dynamicMetadata").getJSONObject(fieldName).getJSONObject("values").get("name").toString();

                            if (!actualShowPageValue.contains(expectedColumnValue)) {
                                customAssert.assertTrue(false, "After Bulk Create Show Page Dynamic Field " + fieldName + " not matched with expected Value");
                            }
                        }

                    }
                }


            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Show Page For entity Type Id " + entityTypeId + " entity Id " + entityId);
        }
    }

    private void validateCustomFieldShowPageValues(int entityTypeId,int entityId,Map<String,String> expectedColValues,CustomAssert customAssert){

        Show show = new Show();

        try {
            show.hitShowVersion2(entityTypeId, entityId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);
            JSONObject dataJson = showResponseJson.getJSONObject("body").getJSONObject("data");

            HashMap<Integer, String> fieldIdNameMap = ShowHelper.getFieldIdNameMap(entityTypeId, entityId, customAssert);

            for (Map.Entry<String, String> entry : expectedColValues.entrySet()) {

                int columnId = Integer.parseInt(entry.getKey());
                String expectedColumnValue = entry.getValue();

                if (columnId == 100000001 || columnId == 100000002) {
                    continue;
                } else {
                    if (fieldIdNameMap.containsKey(columnId)) {

                        String fieldName = fieldIdNameMap.get(columnId);
                        if (!fieldName.contains("dyn")) {
                            String actualShowPageValue = "";
                            try {
                                actualShowPageValue = dataJson.getJSONObject(fieldName).getJSONObject("values").get("name").toString();

                            } catch (Exception e1) {
                                try {
                                    actualShowPageValue = dataJson.getJSONObject(fieldName).get("displayValues").toString();
                                } catch (Exception e2) {
                                    try {
                                        actualShowPageValue = dataJson.getJSONObject(fieldName).get("values").toString();
                                    } catch (Exception e3) {
                                        logger.error("Exception while fetching value from show page for the field " + fieldName);
                                    }
                                }
                            }
                            actualShowPageValue = actualShowPageValue.replace(",","");
                            if (!(actualShowPageValue.contains(expectedColumnValue) || expectedColumnValue.contains(actualShowPageValue))) {
                                customAssert.assertTrue(false, "After Bulk Create Show Page Field " + fieldName + " not matched with expected Value for entity type id " + entityTypeId + " and entity id " + entityId);
                            }
                        } else {
                            String actualShowPageValue = "";
                            try{
                                actualShowPageValue = dataJson.getJSONObject("dynamicMetadata").getJSONObject(fieldName).get("values").toString();
                            }catch (Exception e1){
                                try {
                                    actualShowPageValue = dataJson.getJSONObject("dynamicMetadata").getJSONObject(fieldName).getJSONObject("values").get("name").toString();
                                }catch (Exception e2){

                                }

                            }
                            if(APIUtils.validJsonResponse(actualShowPageValue)){
                                try{
                                    JSONArray multiSelectValues = new JSONArray(actualShowPageValue);

                                    for(int i =0;i<multiSelectValues.length();i++){
                                        actualShowPageValue = actualShowPageValue + multiSelectValues.getJSONObject(i).get("name").toString() + ";";
                                    }
                                }catch (Exception e){
                                    logger.error("Exception while fetching multi select values " + e.getStackTrace());
                                }
                                actualShowPageValue = actualShowPageValue.substring(0,actualShowPageValue.length() - 1);
                            }

                            if (!actualShowPageValue.contains(expectedColumnValue)) {
                                customAssert.assertTrue(false, "After Bulk Create Show Page Dynamic Field " + fieldName + " not matched with expected Value");
                            }
                        }

                    }
                }


            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Custom Field For Show Page For entity Type Id " + entityTypeId + " entity Id " + entityId + " After Bulk Create");
        }
    }

    private void validateLocalListing(int newlyCreatedRecordId, String entityName, int parentEntityTypeId, int parentId, int entityTypeId, CustomAssert csAssert) {
        try {
            logger.info("Validating Local Listing of Newly Created Record Id {} of Entity {}", newlyCreatedRecordId, entityName);
            Map<String, String> paramsForListData = new HashMap<>();

            switch (parentEntityTypeId) {
                case 61:
                    paramsForListData.put("contractId", String.valueOf(parentId));
                    break;

                case 1:
                    paramsForListData.put("relationId", String.valueOf(parentId));
                    break;

                default:
                    paramsForListData = null;
            }

            String listDataResponse = ListDataHelper.getListDataResponse(entityName, ListDataHelper.getDefaultPayloadForListData(entityTypeId, 1),
                    paramsForListData);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                JSONObject jsonObj = new JSONObject(listDataResponse);
                String idValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                int actualRecordId = ListDataHelper.getRecordIdFromValue(idValue);

                if (actualRecordId != newlyCreatedRecordId) {
                    csAssert.assertTrue(false, "Local Listing Validation Failed for Newly Created Record Id " + newlyCreatedRecordId +
                            " of Entity " + entityName + ". Expected Record Id: " + newlyCreatedRecordId + " and Actual Record Id found: " + actualRecordId);
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Local Listing of Newly Created Record Id " + newlyCreatedRecordId + " of Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

}
