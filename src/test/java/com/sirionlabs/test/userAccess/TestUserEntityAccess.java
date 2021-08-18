package com.sirionlabs.test.userAccess;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

public class TestUserEntityAccess {

    private final static Logger logger = LoggerFactory.getLogger(TestUserEntityAccess.class);

    private String configFilePath;
    private String configFileName;
    private int supplierEntityTypeId = 1;
    private List<String> entitiesToCheckForVendor;


    private String reportFilePath;
    private String reportFileName;
    private String reportFileSheetName;

    int rowNum = 1;
    Map<Integer,String> supplierMap = new HashMap<>();

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestUserEntityAccessFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestUserEntityAccessFileName");

        entitiesToCheckForVendor = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entities to check for vendor id").split(","));

        reportFilePath = "src/test/output/Miscellaneous/UserAccessReport";
        reportFileName = "UserAccessReport1.xlsx";
        reportFileSheetName = "Sheet1";

//        FileUtils.deleteFile(reportFilePath,reportFileName);
        try {
            Workbook wb = new XSSFWorkbook();
//            XSSFWorkbook wb = new XSSFWorkbook(new File(reportFilePath + "/" + reportFileName));
            // An output stream accepts output bytes and sends them to sink.
//            File fileOut = new File(reportFilePath + "/" + reportFileName);
            FileOutputStream fileOut = new FileOutputStream(reportFilePath + "/" + reportFileName);
            // Creating Sheets using sheet object
            wb.createSheet(reportFileSheetName);

            wb.write(fileOut);
            System.out.println();
        }catch (Exception e){
            logger.error("Error while creating Report File");
        }

    }

    //parallel will be always false
    @DataProvider(parallel = false)
    public Object[][] dataProviderForUserAccess() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entities to test").split(",");

        for (String entity : entitiesToTest) {

            allTestData.add(new Object[]{entity});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(parallel = false)
    public Object[][] dataProviderForUserAccessReports() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entities to test").split(",");

        for (String entity : entitiesToTest) {
            String reportIdsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"report ids to test");

            if(reportIdsToTest!=null){
                String[] reportIdsToTestArray = reportIdsToTest.split(",");

                for(String reportId : reportIdsToTestArray){
                    allTestData.add(new Object[]{entity,reportId});
                }
            }
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForUserAccess",enabled = true)
    public void Test_UserEntityAccess_SupplierTypeUsers_Listing(String entityName){

        CustomAssert customAssert = new CustomAssert();
        try{

            String expectedVendorIdForUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor id for user 1");
            String expectedVendorIdForUser2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor id for user 2");

            String expectedVendorNameForUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor name for user 1");
            String expectedVendorNameForUser2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor name for user 2");

            Check check = new Check();
            String supplierUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 1");
            String passwordSupplierUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 1 password");
            String supplierUser2 =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 2");
            String passwordSupplierUser2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 2 password");

            //Logging with supplier 1
            logger.info("Logging with supplier 1");
            check.hitCheck(supplierUser1,passwordSupplierUser1);

            if(entitiesToCheckForVendor.contains(entityName)){
                checkVendorScenarioListing(entityName,expectedVendorIdForUser1,expectedVendorNameForUser1,customAssert);
            } else {
                checkSupplierScenariosListing(entityName,expectedVendorIdForUser1,expectedVendorNameForUser1,supplierUser1,customAssert);
            }

            logger.info("Logging with supplier 2");
            check.hitCheck(supplierUser2,passwordSupplierUser2);

            if(entitiesToCheckForVendor.contains(entityName)){
                checkVendorScenarioListing(entityName,expectedVendorIdForUser2,expectedVendorNameForUser2,customAssert);
            }else {
                checkSupplierScenariosListing(entityName,expectedVendorIdForUser2,expectedVendorNameForUser2,supplierUser2,customAssert);
            }

        }catch (Exception e){
            customFailure("Exception while validating the scenario",customAssert);
        }

        customAssert.assertAll();
    }


    @Test(dataProvider = "dataProviderForUserAccessReports")
    public void Test_UserEntityAccess_SupplierTypeUsers_Report(String entityName,String reportId){

        CustomAssert customAssert = new CustomAssert();
        try{

            String expectedVendorIdForUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor id for user 1");
            String expectedVendorIdForUser2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor id for user 2");

            String expectedVendorNameForUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor name for user 1");
            String expectedVendorNameForUser2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor name for user 2");

            Check check = new Check();
            String supplierUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 1");
            String passwordSupplierUser1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 1 password");
            String supplierUser2 =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 2");
            String passwordSupplierUser2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier user 2 password");

            //Logging with supplier 1
            logger.info("Logging with supplier 1");
            check.hitCheck(supplierUser1,passwordSupplierUser1);

            if(entitiesToCheckForVendor.contains(entityName)){
                checkVendorScenarioReport(entityName,reportId,expectedVendorIdForUser1,expectedVendorNameForUser1,customAssert);
            } else {
                checkSupplierScenariosReport(entityName,reportId,expectedVendorIdForUser1,expectedVendorNameForUser1,supplierUser1,customAssert);

            }

            logger.info("Logging with supplier 2");
            check.hitCheck(supplierUser2,passwordSupplierUser2);

            if(entitiesToCheckForVendor.contains(entityName)){
                checkVendorScenarioReport(entityName,reportId,expectedVendorIdForUser2,expectedVendorNameForUser2,customAssert);
            } else {
                checkSupplierScenariosReport(entityName,reportId,expectedVendorIdForUser2,expectedVendorNameForUser2,supplierUser2,customAssert);

            }

        }catch (Exception e){
            customFailure("Exception while validating the scenario",customAssert);
        }

        customAssert.assertAll();
    }

    @AfterClass
    public void afterClass(){

        Check check = new Check();
        check.hitCheck();
    }

    private void customFailure(String message, CustomAssert customAssert){
        logger.error(message);
        customAssert.assertTrue(false,message);
    }

    private void checkSupplierScenariosListing(String entityName,String expVendorId,String expVendorName,String user,CustomAssert customAssert){

        ListRendererListData listRendererListData = new ListRendererListData();
        String payload = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"payload");
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);
        String entityShortCode = ConfigureConstantFields.getShortCodeForEntity(entityName);

        listRendererListData.hitListRendererListDataV2amTrue(listId,payload);
        String listResponse = listRendererListData.getListDataJsonStr();
        Map<Integer, Object> columnDataMap = new HashMap<>();
        columnDataMap.put(0,"Entity Name : " + entityName.toUpperCase());

        if(!JSONUtility.validjson(listResponse)){
            customFailure("List response is an invalid json for user " + user + listResponse,customAssert);
        }else {

            JSONObject listResponseJson = new JSONObject(listResponse);
            String supplierColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "supplier column id");
            String idColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "id column id");
            JSONArray dataArray = listResponseJson.getJSONArray("data");
            if (dataArray.length() == 0) {
                customFailure("Listing Response has zero data for user " + user + " Please check if no data is there or due to some bug", customAssert);
            }

            String relationId;
            String supplierName;
            String entityId;
            String entityIdListing;
            int supplierId = -1;
            String errorMsg = "";

            for (int i = 0; i < dataArray.length(); i++) {

                relationId = dataArray.getJSONObject(i).getJSONObject(supplierColId).get("value").toString().split(":;")[1];
                supplierName = dataArray.getJSONObject(i).getJSONObject(supplierColId).get("value").toString().split(":;")[0];
                entityId = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[1];
                entityIdListing = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[0];

                try {
                    supplierId = Integer.parseInt(relationId);

                } catch (Exception e) {
                    customFailure("Value of relation id is not an integer Relation ID Value From Listing " + relationId, customAssert);
                    customAssert.assertAll();
                    return;
                }

                if(entityIdListing.length()>= 5 ){
                    entityId = entityShortCode + entityIdListing;
                }else if(entityIdListing.length()< 5 ){
                    String zeroString = "";
                    if(entityIdListing.length() == 4){
                        zeroString = "0";
                    }else if(entityIdListing.length() == 3){
                        zeroString = "00";
                    }else if(entityIdListing.length() == 2){
                        zeroString = "000";
                    }else if(entityIdListing.length() == 1){
                        zeroString = "0000";
                    }
                    entityId = entityShortCode + zeroString + entityIdListing;

                }
                String vendorID = ShowHelper.getValueOfField(supplierEntityTypeId, supplierId, "vendor id");
                String vendorName = ShowHelper.getValueOfField(supplierEntityTypeId, supplierId, "vendor");

//                if(!supplierMap.containsKey(supplierId)){
//                    supplierName = ShowHelper.getValueOfField(supplierEntityTypeId, supplierId, "name");
//                    supplierMap.put(supplierId,supplierName);
//                }else {
//                    supplierName = supplierMap.get(supplierId);
//                }

                if(vendorID == null){
//                    errorMsg = "While hitting Show page of supplier for " + entityName + " either vendor ID is null or supplier do not have access to this vendorID" + " for Entity " + entityName + " Entity Id " + entityId + " But it is displayed in listing page";
                    errorMsg = "For Entity Id : " + entityId + ", supplier : \"" + supplierName + "\" does not have access to the " + entityName + " but record is displayed in listing page";

                    columnDataMap.put(1,errorMsg);
                    XLSUtils.editRowData(reportFilePath,reportFileName, reportFileSheetName, rowNum++, columnDataMap);
                    customFailure(errorMsg,customAssert);
                    continue;
                }else if (!vendorID.equalsIgnoreCase(expVendorId)) {

//                    errorMsg = "For Entity Id " + entityId + " Actual Vendor ID " + vendorID + " Expected Vendor ID " + expVendorId;
                    errorMsg = "For Entity Id : " + entityId + " Actual Vendor ID \"" + vendorName + "\" Expected Vendor ID \"" + expVendorName + "\"";
                    columnDataMap.put(1,errorMsg);
                    XLSUtils.editRowData(reportFilePath,reportFileName, reportFileSheetName, rowNum++, columnDataMap);

                    customFailure(errorMsg, customAssert);
                }
            }
        }

    }


    private void checkVendorScenarioListing(String entityName,String expVendorId,String expVendorName,CustomAssert customAssert){

        ListRendererListData listRendererListData = new ListRendererListData();
        String payload = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"payload");
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);
        String entityShortCode = ConfigureConstantFields.getShortCodeForEntity(entityName);

        listRendererListData.hitListRendererListDataV2isFirstCall(listId,payload);
        String listResponse = listRendererListData.getListDataJsonStr();

        if(!JSONUtility.validjson(listResponse)){
            customFailure("List response is an invalid json " + listResponse,customAssert);
        }else {
            JSONObject listResponseJson = new JSONObject(listResponse);
            String vendorColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "vendor column id");
            String idColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "id column id");

            JSONArray dataArray = listResponseJson.getJSONArray("data");
            if (dataArray.length() == 0) {
                customFailure("Listing Response has zero data Please check if no data is there or due to some bug", customAssert);
            }
            String vendorId;
            String vendorName  = "";
            String entityId;
            String entityIdListing;
            String errorMsg = "";
            Map<Integer, Object> columnDataMap = new HashMap<>();
            columnDataMap.put(0,"Entity Name : " + entityName.toUpperCase());

            for (int i = 0; i < dataArray.length(); i++) {

                vendorId = dataArray.getJSONObject(i).getJSONObject(vendorColId).get("value").toString().split(":;")[1];
                vendorName = dataArray.getJSONObject(i).getJSONObject(vendorColId).get("value").toString().split(":;")[0];
                entityId = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[1];
                entityIdListing = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[0];

                if(entityIdListing.length()>= 5 ){
                    entityId = entityShortCode + entityIdListing;
                }else if(entityIdListing.length()< 5 ){
                    String zeroString = "";
                    if(entityIdListing.length() == 4){
                        zeroString = "0";
                    }else if(entityIdListing.length() == 3){
                        zeroString = "00";
                    }else if(entityIdListing.length() == 2){
                        zeroString = "000";
                    }else if(entityId.length() == 1){
                        zeroString = "0000";
                    }
                    entityId = entityShortCode + zeroString + entityIdListing;

                }

                try{
                    if (!vendorId.equalsIgnoreCase(expVendorId)) {
//                        errorMsg = "For Entity Id " + entityId + " Actual Vendor ID " + vendorId + " Expected Vendor ID " + expVendorId;
                        errorMsg = "For Entity Id : \"" + entityId + "\" Actual Vendor Name \"" + vendorName + "\" Expected Vendor Name \"" + expVendorName + "\"";
                        columnDataMap.put(1,errorMsg);
                        XLSUtils.editRowData(reportFilePath,reportFileName, reportFileSheetName, rowNum++, columnDataMap);
                        customFailure(errorMsg, customAssert);

                    }
                }catch (Exception e){
                    customFailure("Exception while validating Vendor Id on listing with expected vendor id",customAssert);
                }

            }
        }
    }

    private void checkVendorScenarioReport(String entityName,String reportId,String expVendorId,String expVendorName,CustomAssert customAssert){

        ReportRendererListData reportRendererListData = new ReportRendererListData();
        String reportPayload = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"report payload " + reportId);


        String entityShortCode = ConfigureConstantFields.getShortCodeForEntity(entityName);

        reportRendererListData.hitReportRendererListData(Integer.parseInt(reportId),reportPayload);
        String listResponse = reportRendererListData.getListDataJsonStr();

        if(!JSONUtility.validjson(listResponse)){
            customFailure("List response is an invalid json for entity " + entityName + " and report id " + reportId + listResponse,customAssert);
        }else {
            JSONObject listResponseJson = new JSONObject(listResponse);
            String vendorColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "vendor column id");
            String idColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "id column id");

            JSONArray dataArray = listResponseJson.getJSONArray("data");
            if (dataArray.length() == 0) {
                customFailure("Listing Response has zero data Please check if no data is there or due to some bug", customAssert);
            }
            String vendorId;
            String vendorName  = "";
            String entityId;
            String entityIdListing;
            String errorMsg = "";
            Map<Integer, Object> columnDataMap = new HashMap<>();
            columnDataMap.put(0,"Entity Name : " + entityName.toUpperCase());

            for (int i = 0; i < dataArray.length(); i++) {

                vendorId = dataArray.getJSONObject(i).getJSONObject(vendorColId).get("value").toString().split(":;")[1];
                vendorName = dataArray.getJSONObject(i).getJSONObject(vendorColId).get("value").toString().split(":;")[0];
                entityId = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[1];
                entityIdListing = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[0];

                if(entityIdListing.length()>= 5 ){
                    entityId = entityShortCode + entityIdListing;
                }else if(entityIdListing.length()< 5 ){
                    String zeroString = "";
                    if(entityIdListing.length() == 4){
                        zeroString = "0";
                    }else if(entityIdListing.length() == 3){
                        zeroString = "00";
                    }else if(entityIdListing.length() == 2){
                        zeroString = "000";
                    }else if(entityId.length() == 1){
                        zeroString = "0000";
                    }
                    entityId = entityShortCode + zeroString + entityIdListing;

                }

                try{
                    if (!vendorId.equalsIgnoreCase(expVendorId)) {
//                        errorMsg = "For Entity Id " + entityId + " Actual Vendor ID " + vendorId + " Expected Vendor ID " + expVendorId;
                        errorMsg = "For Entity Id : \"" + entityId + "\" Actual Vendor Name \"" + vendorName + "\" Expected Vendor Name \"" + expVendorName + "\"";
                        columnDataMap.put(1,errorMsg);
                        XLSUtils.editRowData(reportFilePath,reportFileName, reportFileSheetName, rowNum++, columnDataMap);
                        customFailure(errorMsg, customAssert);

                    }
                }catch (Exception e){
                    customFailure("Exception while validating Vendor Id on listing with expected vendor id",customAssert);
                }

            }
        }
    }

    private void checkSupplierScenariosReport(String entityName,String reportId,String expVendorId,String expVendorName,String user,CustomAssert customAssert){

        ReportRendererListData reportRendererListData = new ReportRendererListData();
        String reportPayload = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"report payload " + reportId);


        String entityShortCode = ConfigureConstantFields.getShortCodeForEntity(entityName);

        reportRendererListData.hitReportRendererListData(Integer.parseInt(reportId),reportPayload);
        String reportListResponse = reportRendererListData.getListDataJsonStr();

        Map<Integer, Object> columnDataMap = new HashMap<>();
        columnDataMap.put(0,"Entity Name : " + entityName.toUpperCase());

        if(!JSONUtility.validjson(reportListResponse)){
            customFailure("Report List response is an invalid json for user " + user + reportListResponse,customAssert);
        }else {

            JSONObject listResponseJson = new JSONObject(reportListResponse);
            String supplierColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "supplier column id report " + reportId);
            String idColId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "id column id report " + reportId);
            JSONArray dataArray = listResponseJson.getJSONArray("data");
            if (dataArray.length() == 0) {
                customFailure("Listing Response has zero data for user " + user + " Please check if no data is there or due to some bug", customAssert);
            }

            String relationId;
            String supplierName;
            String entityId;
            String entityIdListing;
            int supplierId = -1;
            String errorMsg = "";

            for (int i = 0; i < dataArray.length(); i++) {

                relationId = dataArray.getJSONObject(i).getJSONObject(supplierColId).get("value").toString().split(":;")[1];
                supplierName = dataArray.getJSONObject(i).getJSONObject(supplierColId).get("value").toString().split(":;")[0];
                entityId = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[1];
                entityIdListing = dataArray.getJSONObject(i).getJSONObject(idColId).get("value").toString().split(":;")[0];

                try {
                    supplierId = Integer.parseInt(relationId);

                } catch (Exception e) {
                    customFailure("Value of relation id is not an integer Relation ID Value From Listing " + relationId, customAssert);
                    customAssert.assertAll();
                    return;
                }

                if(entityIdListing.length()>= 5 ){
                    entityId = entityShortCode + entityIdListing;
                }else if(entityIdListing.length()< 5 ){
                    String zeroString = "";
                    if(entityIdListing.length() == 4){
                        zeroString = "0";
                    }else if(entityIdListing.length() == 3){
                        zeroString = "00";
                    }else if(entityIdListing.length() == 2){
                        zeroString = "000";
                    }else if(entityIdListing.length() == 1){
                        zeroString = "0000";
                    }
                    entityId = entityShortCode + zeroString + entityIdListing;

                }
                String vendorID = ShowHelper.getValueOfField(supplierEntityTypeId, supplierId, "vendor id");
                String vendorName = ShowHelper.getValueOfField(supplierEntityTypeId, supplierId, "vendor");

//                if(!supplierMap.containsKey(supplierId)){
//                    supplierName = ShowHelper.getValueOfField(supplierEntityTypeId, supplierId, "name");
//                    supplierMap.put(supplierId,supplierName);
//                }else {
//                    supplierName = supplierMap.get(supplierId);
//                }

                if(vendorID == null){
//                    errorMsg = "While hitting Show page of supplier for " + entityName + " either vendor ID is null or supplier do not have access to this vendorID" + " for Entity " + entityName + " Entity Id " + entityId + " But it is displayed in listing page";
                    errorMsg = "For Entity Id : " + entityId + ", supplier : \"" + supplierName + "\" does not have access to the " + entityName + " but record is displayed in listing page";

                    columnDataMap.put(1,errorMsg);
                    XLSUtils.editRowData(reportFilePath,reportFileName, reportFileSheetName, rowNum++, columnDataMap);
                    customFailure(errorMsg,customAssert);
                    continue;
                }else if (!vendorID.equalsIgnoreCase(expVendorId)) {

//                    errorMsg = "For Entity Id " + entityId + " Actual Vendor ID " + vendorID + " Expected Vendor ID " + expVendorId;
                    errorMsg = "For Entity Id : " + entityId + " Actual Vendor ID \"" + vendorName + "\" Expected Vendor ID \"" + expVendorName + "\"";
                    columnDataMap.put(1,errorMsg);
                    XLSUtils.editRowData(reportFilePath,reportFileName, reportFileSheetName, rowNum++, columnDataMap);

                    customFailure(errorMsg, customAssert);
                }
            }
        }

    }

}
