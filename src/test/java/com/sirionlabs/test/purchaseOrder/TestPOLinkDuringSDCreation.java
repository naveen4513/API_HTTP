package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.bulkedit.BulkeditCreate;
import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
import com.sirionlabs.helper.entityCreation.ServiceData;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestPOLinkDuringSDCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestPOLinkDuringSDCreation.class);

    private static String configFilePath;
    private static String configFileName;

    private static String poCreationFilePath;
    private static String poCreationFileName;

    private String poExtraFieldsConfigFilePath;
    private String poExtraFieldsConfigFileName;

    private int poEntityTypeId;

    private static String sdCreationFilePath;
    private static String sdCreationFileName;

    private String sdExtraFieldsConfigFilePath;
    private String sdExtraFieldsConfigFileName;

    private int sdEntityTypeId;

    private String serviceData = "service data";

    private String clientId;
    private String bulkCreateConfigFilePath;
    private String bulkCreateConfigFileName;

    private String poFieldId;
    String bulkCreateExcelFilePath;

    @BeforeClass
    public void beforeClass(){

        poCreationFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFilePath");
        poCreationFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFileName");
        poExtraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFilePath");
        poExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderExtraFieldsFileName");
        poEntityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");

        sdCreationFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFilePath");
        sdCreationFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFileName");
        sdExtraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFilePath");
        sdExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");
        sdEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

        clientId = ConfigureEnvironment.getEnvironmentProperty("client_id");

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("POLinkSDConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("POLinkSDConfigFileName");
        bulkCreateConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateConfigFilePath");
        bulkCreateConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateConfigFileName");

        poFieldId = "12807";

    }

    @Test(enabled = true)
    public void Test_POLinkDuringSDCreation() {

        CustomAssert customAssert = new CustomAssert();
        String poFlow = "po sd link flow";
        String serviceDataFlow = "po sd link flow arc flow create";
        int purchaseOrderId = -1;
        int numberOfPOToCreate = 3;
        ArrayList<Integer> expectedPoList = new ArrayList<>();

        try {
            String uniqueString = DateUtils.getCurrentTimeStamp();
            for(int i =1;i<=numberOfPOToCreate;i++) {

                uniqueString = DateUtils.getCurrentTimeStamp();
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
                String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
                        true);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                        if (purchaseOrderId == -1) {

                            customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
                            customAssert.assertAll();
                            return;
                        }else {
                            expectedPoList.add(purchaseOrderId);
                        }

                    } else {
                        customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
                    }
                }
            }
            String poString = "";

            for(Integer poId : expectedPoList) {
                poString += "{\"name\":\"purchase order test\",\"id\":" + poId + ",\"entityTypeId\":181,\"clientId\":" + clientId + ",\"description\":\"po number\",\"multiplier\":1,\"entityFinancialData\":{\"id\":null}}" + ",";
            }

            poString = poString.substring(0,poString.length() - 1);
            UpdateFile.updateConfigFileProperty(sdExtraFieldsConfigFilePath, sdExtraFieldsConfigFileName, serviceDataFlow, "purchaseOrder", "poString", poString);
            int serviceDataId = InvoiceHelper.getServiceDataId(sdCreationFilePath, sdCreationFileName, sdExtraFieldsConfigFileName, serviceDataFlow, uniqueString);
            UpdateFile.updateConfigFileProperty(sdExtraFieldsConfigFilePath, sdExtraFieldsConfigFileName, serviceDataFlow, "purchaseOrder",  poString,"poString");

            validate_POIdsOnShowPage(serviceDataId,expectedPoList,customAssert);

            validate_POIdsOnSDListPage(expectedPoList,customAssert);

        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getStackTrace());
        }
        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void Test_POLinkDuringSDEdit(){

        CustomAssert customAssert = new CustomAssert();

        try{
            String poFlow = "po sd link flow";
            String serviceDataFlow = "po sd link flow fixed fee flow edit";

            int numberOfPOToCreate = 3;
            int purchaseOrderId = -1;
            ArrayList<Integer> expectedPoList = new ArrayList<>();

            String uniqueString = DateUtils.getCurrentTimeStamp();
            for(int i =1;i<numberOfPOToCreate;i++) {

                uniqueString = DateUtils.getCurrentTimeStamp();
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
                String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
                        true);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                        if (purchaseOrderId == -1) {

                            customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
                            customAssert.assertAll();
                            return;
                        }else {
                            expectedPoList.add(purchaseOrderId);
                        }

                    } else {
                        customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
                    }
                }
            }

            String poString = "";
            for(Integer poId : expectedPoList) {
                poString += "{\"name\":\"purchase order test\",\"id\":" + poId + ",\"entityTypeId\":181,\"clientId\":" + clientId + ",\"description\":\"po number\",\"multiplier\":1,\"entityFinancialData\":{\"id\":null}}" + ",";
            }

            poString = poString.substring(0,poString.length() - 1);

            int serviceDataId = InvoiceHelper.getServiceDataId(sdCreationFilePath, sdCreationFileName, sdExtraFieldsConfigFileName, serviceDataFlow, uniqueString);

            Edit edit = new Edit();
            String editPayload =  edit.getEditPayload(serviceData,serviceDataId);

            if(JSONUtility.validjson(editPayload)){

                JSONObject editPayloadJson = new JSONObject(editPayload);
                JSONArray valuesArray = new JSONArray("[" + poString + "]");

                editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("purchaseOrder").put("values",valuesArray);

                String editResponse = edit.hitEdit(serviceData,editPayloadJson.toString());

                if(editResponse.contains("success")) {
                    validate_POIdsOnShowPage(serviceDataId, expectedPoList, customAssert);

                    validate_POIdsOnSDListPage(expectedPoList,customAssert);

                }else {
                    customAssert.assertTrue(false,"Edit done unsuccessfully on service data while editing PO ");
                }

            }else {
                customAssert.assertTrue(false,"Edit Payload is an invalid json");
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void test_POLinkDuringBulkCreateSD() {
        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "service data po link";
        String poFlow = "po sd link flow";

        int numberOfPOToCreate = 1;
        int purchaseOrderId = -1;

        ArrayList<Integer> expectedPoList = new ArrayList<>();

        BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

        try {

            String uniqueString = DateUtils.getCurrentTimeStamp();
            for(int i =1;i<=numberOfPOToCreate;i++) {

                uniqueString = DateUtils.getCurrentTimeStamp();
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
                String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
                        true);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                        if (purchaseOrderId == -1) {

                            customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
                            customAssert.assertAll();
                            return;
                        }else {
                            expectedPoList.add(purchaseOrderId);
                        }

                    } else {
                        customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
                    }
                }
            }

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

            if (flowProperties.isEmpty()) {
                throw new SkipException("Couldn't get All Properties of Flow " + flowToTest);
            }

            Map<String,String> defaultProperties = ParseConfigFile.getAllDefaultProperties(bulkCreateConfigFilePath, bulkCreateConfigFileName);

            bulkCreateExcelFilePath = defaultProperties.get("excelfilepath");
            Long schedulerJobTimeOut = Long.parseLong(defaultProperties.get("schedulerwaittimeout"));
            Long schedulerJobPollingTime = Long.parseLong(defaultProperties.get("schedulerpollingtime"));

            logger.info("Verifying Bulk Create for Flow: [{}]", flowToTest);

            String testTemplateName = flowToTest + ".xlsm";

            int parentEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "parentEntityTypeId"));
            int parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "parentId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "templateId"));

            BulkTemplate.downloadBulkCreateTemplate(bulkCreateExcelFilePath, testTemplateName, templateId,  parentEntityTypeId, parentId);
            String sheetName = flowProperties.get("sheet name");
            if(flowProperties.get("unique fields") != null){
                String[] uniqueFields = flowProperties.get("unique fields").split(",");

                updateUniqueFields(bulkCreateExcelFilePath,testTemplateName,sheetName,uniqueFields);

            }

            Map<String,String> dataMapForExcelFile = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,"fields to update");
            List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));

            Map<String, Object> dataMap= new HashMap<>();;

            for (Map.Entry<String, String> entry : dataMapForExcelFile.entrySet()) {
                if (NumberUtils.isParsable(entry.getValue())) {
                    if (dateColumnIds.contains(entry.getKey())) {
                        dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                    } else
                        dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                } else {
                    if (dateColumnIds.contains(entry.getKey())) {
                        SimpleDateFormat date = new SimpleDateFormat(entry.getValue());
                        dataMap.put(entry.getKey(), date);
                    }else {
                        dataMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            String pONameValueInExcel = "";
            for(Integer poId : expectedPoList){
                pONameValueInExcel += ShowHelper.getValueOfField(poEntityTypeId,poId,"name") + ";";
            }
            pONameValueInExcel = pONameValueInExcel.substring(0,pONameValueInExcel.length() -1);

            dataMap.put("12807",pONameValueInExcel);

            XLSUtils.editRowDataUsingColumnId(bulkCreateExcelFilePath,testTemplateName,sheetName,6,dataMap);

            String entityName = flowProperties.get("entity");
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            //Upload Bulk Create Template
            String bulkCreateUploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateExcelFilePath, testTemplateName, parentEntityTypeId, parentId,
                    entityTypeId, templateId);

            if (bulkCreateUploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    int newlyCreatedServiceDataId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);

                    if (newlyCreatedServiceDataId == -1) {
                        throw new SkipException("Couldn't get Newly Created Record Id from Bulk Template.");
                    }else {

                        validate_POIdsOnShowPage(newlyCreatedServiceDataId,expectedPoList,customAssert);

                        validate_POIdsOnSDListPage(expectedPoList,customAssert);

                    }


                } else {
                    String errorMessage = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);
                    customAssert.assertTrue(false, "Bulk Create Scheduler Job Failed whereas it was supposed to Pass. Error Message: " + errorMessage);
                }
            } else {
                customAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + bulkCreateUploadResponse + "]");
            }

            FileUtils.deleteFile(bulkCreateExcelFilePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Verifying BulkCreate Flow [" + flowToTest + "]. " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void test_POLinkDuringBulkUpdateSD(){

        CustomAssert customAssert = new CustomAssert();
        String poFlow = "po sd link flow";
        String serviceDataFlow = "po sd link flow arc flow edit";
        String bulkUpdateFlow = "service data po link bulk update";
//        String serviceDataFlow = "arc flow 1";
        int purchaseOrderId = -1;
        int numberOfPOToCreate = 1;

        ArrayList<Integer> expectedPoList = new ArrayList<>();
        BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

        try {
            String uniqueString = DateUtils.getCurrentTimeStamp();
            for(int i =1;i<=numberOfPOToCreate;i++) {

                uniqueString = DateUtils.getCurrentTimeStamp();
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
                String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
                        true);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                        if (purchaseOrderId == -1) {

                            customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
                            customAssert.assertAll();
                            return;
                        }else {
                            expectedPoList.add(purchaseOrderId);
                        }

                    } else {
                        customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
                    }
                }
            }
            String poString = "";

            for(Integer poId : expectedPoList) {
                poString += "{\"name\":\"purchase order test\",\"id\":" + poId + ",\"entityTypeId\":181,\"clientId\":" + clientId + ",\"description\":\"po number\",\"multiplier\":1,\"entityFinancialData\":{\"id\":null}}" + ",";
            }
            poString = poString.substring(0,poString.length() - 1);

            UpdateFile.updateConfigFileProperty(sdExtraFieldsConfigFilePath, sdExtraFieldsConfigFileName, serviceDataFlow, "purchaseOrder",  "poString",poString);
            int serviceDataId = InvoiceHelper.getServiceDataId(sdCreationFilePath, sdCreationFileName, sdExtraFieldsConfigFileName, serviceDataFlow, uniqueString);

            if(serviceDataId == -1){
                customAssert.assertTrue(false,"Service Data not created for the flow " + serviceDataFlow);
                customAssert.assertAll();
                return;
            }


            String bulkUpdateExcelFilePath = "src\\test\\resources\\TestConfig\\BulkUpdate\\ExcelSheets";
            String bulkUpdateExcelFileName = serviceDataFlow + ".xlsm";

            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service data po link bulk update","templateid"));
            BulkTemplate.downloadBulkUpdateTemplate(bulkUpdateExcelFilePath, bulkUpdateExcelFileName, templateId,  sdEntityTypeId, String.valueOf(serviceDataId));

            Map<String, Object> columnDataMap = new HashMap<>();

            String pONameValueInExcel = "";
            for(Integer poId : expectedPoList){
                pONameValueInExcel += ShowHelper.getValueOfField(poEntityTypeId,poId,"name") + ";";
            }
            pONameValueInExcel = pONameValueInExcel.substring(0,pONameValueInExcel.length() -1);

            columnDataMap.put(poFieldId,pONameValueInExcel);

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, bulkUpdateFlow);
            String sheetName = flowProperties.get("sheet name");

            XLSUtils.editRowDataUsingColumnId(bulkUpdateExcelFilePath,bulkUpdateExcelFileName,sheetName,6,columnDataMap);
            logger.info("Killing All Scheduler Tasks for Flow [{}].", serviceDataFlow);
            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            //Upload Bulk Create Template
            String bulkCreateUploadResponse = BulkTemplate.uploadBulkUpdateTemplate(bulkUpdateExcelFilePath, bulkUpdateExcelFileName,
                    sdEntityTypeId, templateId);

            if (bulkCreateUploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Update Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Long schedulerJobTimeOut = 120000L;
                Long schedulerJobPollingTime = 5000L;

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }else if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {

                    validate_POIdsOnShowPage(serviceDataId,expectedPoList,customAssert);

                    validate_POIdsOnSDListPage(expectedPoList,customAssert);

                } else {
                    String errorMessage = bulkHelperObj.getErrorMessagesForBulkCreateFileName(bulkUpdateExcelFileName);
                    customAssert.assertTrue(false, "Bulk Update Scheduler Job Failed whereas it was supposed to Pass. Error Message: " + errorMessage);
                }
            } else {
                customAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + bulkCreateUploadResponse + "]");
            }


        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getStackTrace());
        }
        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void test_POLinkDuringBulkEditSD(){

        CustomAssert customAssert = new CustomAssert();
        String poFlow = "po sd link flow";
        String serviceDataFlow = "po sd link flow arc flow edit";

        int purchaseOrderId = -1;
        int numberOfPOToCreate = 1;

        ArrayList<Integer> expectedPoList = new ArrayList<>();
        BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

        try {
            String uniqueString = DateUtils.getCurrentTimeStamp();

            uniqueString = DateUtils.getCurrentTimeStamp();
            UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
            UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
            String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
                    true);
            UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
            UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                if (createStatus.trim().equalsIgnoreCase("success")) {
                    purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                    if (purchaseOrderId == -1) {

                        customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
                        customAssert.assertAll();
                        return;
                    }else {
                        expectedPoList.add(purchaseOrderId);
                    }

                } else {
                    customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
                }
            }

            String poString = "";

            for(Integer poId : expectedPoList) {
                poString += "{\"name\":\"purchase order test\",\"id\":" + poId + ",\"entityTypeId\":181,\"clientId\":" + clientId + ",\"description\":\"po number\",\"multiplier\":1,\"entityFinancialData\":{\"id\":null}}" + ",";
            }
            poString = poString.substring(0,poString.length() - 1);

            UpdateFile.updateConfigFileProperty(sdExtraFieldsConfigFilePath, sdExtraFieldsConfigFileName, serviceDataFlow, "purchaseOrder",  "poString",poString);
            int serviceDataId = InvoiceHelper.getServiceDataId(sdCreationFilePath, sdCreationFileName, sdExtraFieldsConfigFileName, serviceDataFlow, uniqueString);

            if(serviceDataId == -1){
                customAssert.assertTrue(false,"Service Data not created for the flow " + serviceDataFlow);
                customAssert.assertAll();
                return;
            }

            String pONameValueInExcel = "";

            pONameValueInExcel += ShowHelper.getValueOfField(poEntityTypeId,purchaseOrderId,"name") + ";";

            pONameValueInExcel = pONameValueInExcel.substring(0,pONameValueInExcel.length() -1);

            logger.info("Killing All Scheduler Tasks for Flow [{}].", serviceDataFlow);
            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String bulkEditPayload = "{\"body\":{\"data\":{\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"multiEntitySupport\":false}," +
                    "\"functions\":{\"name\":\"functions\",\"id\":11731,\"multiEntitySupport\":false}," +
                    "\"endDate\":{\"name\":\"endDate\",\"id\":8055,\"multiEntitySupport\":false}," +
                    "\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false}," +
                    "\"parentEntityId\":{\"name\":\"parentEntityId\",\"multiEntitySupport\":false}," +
                    "\"globalRegions\":{\"name\":\"globalRegions\",\"id\":11733,\"multiEntitySupport\":false}," +
                    "\"globalCountries\":{\"name\":\"globalCountries\",\"id\":11343,\"multiEntitySupport\":false}," +
                    "\"supplierAccess\":{\"name\":\"supplierAccess\",\"multiEntitySupport\":false}," +
                    "\"splitAttributeAvailable\":{\"name\":\"splitAttributeAvailable\",\"values\":false,\"multiEntitySupport\":false}," +
                    "\"conversionData\":{\"name\":\"conversionData\",\"id\":11657,\"options\":null,\"multiEntitySupport\":false}," +
                    "\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false}," +
                    "\"arcRrcFrequency\":{\"name\":\"arcRrcFrequency\",\"multiEntitySupport\":false}," +
                    "\"supplier\":{\"name\":\"supplier\",\"id\":11767,\"multiEntitySupport\":false}," +
                    "\"splitRatioType\":{\"name\":\"splitRatioType\",\"id\":11661,\"multiEntitySupport\":false}," +
                    "\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false}," +
                    "\"serviceDataServiceCategory\":{\"name\":\"serviceDataServiceCategory\",\"id\":11766,\"options\":null,\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"multiEntitySupport\":false}," +
                    "\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false}," +
                    "\"state\":{\"name\":\"state\",\"multiEntitySupport\":false}," +
                    "\"contractingHubs\":{\"name\":\"contractingHubs\",\"multiEntitySupport\":false}," +
                    "\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false}," +
                    "\"billingPeriod\":{\"name\":\"billingPeriod\",\"id\":11431,\"multiEntitySupport\":false}," +
                    "\"pricingForReporting\":{\"name\":\"pricingForReporting\",\"id\":12617,\"values\":false,\"multiEntitySupport\":false}," +
                    "\"contract\":{\"name\":\"contract\",\"id\":4040,\"options\":null,\"multiEntitySupport\":false}," +
                    "\"pricingAvailable\":{\"name\":\"pricingAvailable\",\"id\":11430,\"values\":false,\"multiEntitySupport\":false}," +
                    "\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false}," +
                    "\"timeZone\":{\"name\":\"timeZone\",\"multiEntitySupport\":false},\"active\":{\"name\":\"active\",\"values\":false,\"multiEntitySupport\":false}," +
                    "\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false}," +
                    "\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false}," +
                    "\"arcRrcAvailable\":{\"name\":\"arcRrcAvailable\",\"values\":false,\"multiEntitySupport\":false}," +
                    "\"contractService\":{\"name\":\"contractService\",\"id\":4039,\"options\":null,\"multiEntitySupport\":false}," +
                    "\"projectLevels\":{\"name\":\"projectLevels\",\"multiEntitySupport\":false}," +
                    "\"startDate\":{\"name\":\"startDate\",\"id\":8054,\"multiEntitySupport\":false}," +
                    "\"status\":{\"name\":\"status\",\"id\":11775,\"multiEntitySupport\":false}," +
                    "\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"multiEntitySupport\":false}," +
                    "\"nextBillingDate\":{\"name\":\"nextBillingDate\",\"multiEntitySupport\":false}," +
                    "\"serviceIdSupplier\":{\"name\":\"serviceIdSupplier\",\"id\":11041,\"multiEntitySupport\":false}," +
                    "\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"multiEntitySupport\":false}," +
                    "\"services\":{\"name\":\"services\",\"id\":11732,\"multiEntitySupport\":false}," +
                    "\"forecastRollingPeriodType\":{\"name\":\"forecastRollingPeriodType\",\"id\":11448,\"multiEntitySupport\":false}," +
                    "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":64,\"multiEntitySupport\":false}," +
                    "\"lastBillingDate\":{\"name\":\"lastBillingDate\",\"multiEntitySupport\":false}," +
                    "\"serviceIdClient\":{\"name\":\"serviceIdClient\",\"id\":11040,\"multiEntitySupport\":false}," +
                    "\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"rg_2641\":{\"values\":[],\"name\":" +
                    "\"rg_2641\",\"label\":\"Custom Manager\",\"userType\":[2,1,3,4]},\"rg_2667\":{\"values\":[],\"name\":" +
                    "\"rg_2667\",\"label\":\"Sirion Sd Manager\",\"userType\":[1]},\"rg_2666\":{\"values\":[],\"name\":" +
                    "\"rg_2666\",\"label\":\"Client Sd Manager\",\"userType\":[2]}," +
                    "\"rg_2643\":{\"values\":[],\"name\":\"rg_2643\",\"label\":\"Test Manager\",\"userType\":[2,1,3,4]}," +
                    "\"rg_2617\":{\"values\":[],\"name\":\"rg_2617\",\"label\":\"Service Data Manager\",\"userType\":[2,1,3,4]}," +
                    "\"rg_2639\":{\"values\":[],\"name\":\"rg_2639\",\"label\":\"Sd New Manager\",\"userType\":[2,1,3,4]}},\"options\":null,\"multiEntitySupport\":false},\"canSupplierBeParent\":true,\"measurementType\":{\"name\":\"measurementType\",\"multiEntitySupport\":false},\"dynamicMetadata\":{},\"baseSpecificLineItem\":{\"name\":\"baseSpecificLineItem\",\"id\":11455,\"values\":false,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":null,\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false}," +
                    "\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false}}," +
                    "\"purchaseOrder\":{\"name\":\"purchaseOrder\",\"id\":12807,\"options\":null,\"multiEntitySupport\":false," +
                    "\"values\":[{\"name\":\"" + pONameValueInExcel + "\",\"id\":" + purchaseOrderId + ",\"entityTypeId\":181,\"active\":false,\"blocked\":false,\"createdFromListPage\":false,\"summaryGroupData\":false,\"bulkOperation\":false,\"blockedForBulk\":false,\"autoExtracted\":false,\"dynamicFieldsEncrypted\":false,\"systemAdmin\":false,\"canOverdue\":false,\"autoCreate\":false,\"draftEntity\":false,\"validationError\":false,\"isReject\":false,\"parentHalting\":false,\"autoTaskFailed\":false,\"compareHistory\":false,\"flagForClone\":false,\"createStakeHolder\":false,\"escapeValueUpdateTask\":false,\"excludeFromHoliday\":false,\"excludeWeekends\":false,\"datetimeEnabled\":false,\"uploadAllowed\":false,\"downloadAllowed\":false,\"signatureAllowed\":false,\"saveCommentDocOnValueUpdate\":false,\"sourceOfAction\":0,\"savedAsDraft\":false,\"performedInMonth\":false,\"multiplier\":1,\"entityFinancialData\":{\"id\":null,\"entityId\":null,\"entityTypeId\":null,\"totalDirectAcv\":0,\"totalDirectTcv\":0,\"totalDirectFycv\":0,\"totalIndirectAcv\":0,\"totalIndirectTcv\":0,\"totalIndirectFycv\":0,\"dateCreated\":null,\"dateModified\":null,\"deleted\":false},\"deleteProcessed\":false,\"financialParamsUpdated\":false,\"overdue\":false,\"autoTask\":false}]},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null,\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false},\"invoiceCopy\":{\"name\":\"invoiceCopy\",\"values\":false,\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null,\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"values\":[]}}}," +
                    "\"globalData\":{\"entityIds\":[" + serviceDataId + "],\"fieldIds\":[" + poFieldId + "],\"isGlobalBulk\":true}}}";

            BulkeditEdit bulkeditEdit = new BulkeditEdit();
            bulkeditEdit.hitBulkeditEdit(sdEntityTypeId,352, bulkEditPayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {

                customAssert.assertTrue(false, "Bulk edit done unsuccessfully ");
            }else {

                logger.info("Hitting Fetch API to Get Bulk Edit Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Edit Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Long schedulerJobTimeOut = 120000L;
                Long schedulerJobPollingTime = 5000L;

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Edit Scheduler Job didn't finish and hence cannot validate further.");
                } else if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {

                    validate_POIdsOnShowPage(serviceDataId,expectedPoList,customAssert);

                    validate_POIdsOnSDListPage(expectedPoList,customAssert);

                }else {
                    customAssert.assertTrue(false, "Bulk Edit Scheduler Job Failed whereas it was supposed to Pass ");
                }

            }

        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getStackTrace());
        }
        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void test_POLinkDuringBulkCreateInv() {
        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "service data po link";
        String poFlow = "po sd link flow";

        int numberOfPOToCreate = 1;
        int purchaseOrderId = -1;

        ArrayList<Integer> expectedPoList = new ArrayList<>();

        BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

        try {

            String uniqueString = DateUtils.getCurrentTimeStamp();
//            for(int i =1;i<=numberOfPOToCreate;i++) {
//
//                uniqueString = DateUtils.getCurrentTimeStamp();
//                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
//                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
//                String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
//                        true);
//                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
//                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");
//
//                if (ParseJsonResponse.validJsonResponse(createResponse)) {
//                    JSONObject jsonObj = new JSONObject(createResponse);
//                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
//
//                    if (createStatus.trim().equalsIgnoreCase("success")) {
//                        purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");
//
//                        if (purchaseOrderId == -1) {
//
//                            customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
//                            customAssert.assertAll();
//                            return;
//                        }else {
//                            expectedPoList.add(purchaseOrderId);
//                        }
//
//                    } else {
//                        customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
//                    }
//                }
//            }

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

            if (flowProperties.isEmpty()) {
                throw new SkipException("Couldn't get All Properties of Flow " + flowToTest);
            }

            Map<String,String> defaultProperties = ParseConfigFile.getAllDefaultProperties(bulkCreateConfigFilePath, bulkCreateConfigFileName);

            bulkCreateExcelFilePath = defaultProperties.get("excelfilepath");
            Long schedulerJobTimeOut = Long.parseLong(defaultProperties.get("schedulerwaittimeout"));
            Long schedulerJobPollingTime = Long.parseLong(defaultProperties.get("schedulerpollingtime"));

            logger.info("Verifying Bulk Create for Flow: [{}]", flowToTest);

            String testTemplateName = flowToTest + ".xlsm";

            int parentEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "parentEntityTypeId"));
            int parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "parentId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "templateId"));

            BulkTemplate.downloadBulkCreateTemplate(bulkCreateExcelFilePath, testTemplateName, templateId,  parentEntityTypeId, parentId);
            String sheetName = flowProperties.get("sheet name");
            if(flowProperties.get("unique fields") != null){
                String[] uniqueFields = flowProperties.get("unique fields").split(",");

                updateUniqueFields(bulkCreateExcelFilePath,testTemplateName,sheetName,uniqueFields);

            }

            Map<String,String> dataMapForExcelFile = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,"fields to update");
            List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));

            Map<String, Object> dataMap= new HashMap<>();;

            for (Map.Entry<String, String> entry : dataMapForExcelFile.entrySet()) {
                if (NumberUtils.isParsable(entry.getValue())) {
                    if (dateColumnIds.contains(entry.getKey())) {
                        dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                    } else
                        dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                } else {
                    if (dateColumnIds.contains(entry.getKey())) {
                        SimpleDateFormat date = new SimpleDateFormat(entry.getValue());
                        dataMap.put(entry.getKey(), date);
                    }else {
                        dataMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            String pONameValueInExcel = "";
            for(Integer poId : expectedPoList){
                pONameValueInExcel += ShowHelper.getValueOfField(poEntityTypeId,poId,"name") + ";";
            }
            pONameValueInExcel = pONameValueInExcel.substring(0,pONameValueInExcel.length() -1);

            dataMap.put("12807",pONameValueInExcel);

            XLSUtils.editRowDataUsingColumnId(bulkCreateExcelFilePath,testTemplateName,sheetName,6,dataMap);

            String entityName = flowProperties.get("entity");
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            //Upload Bulk Create Template
            String bulkCreateUploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateExcelFilePath, testTemplateName, parentEntityTypeId, parentId,
                    entityTypeId, templateId);

            if (bulkCreateUploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    int newlyCreatedServiceDataId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);

                    if (newlyCreatedServiceDataId == -1) {
                        throw new SkipException("Couldn't get Newly Created Record Id from Bulk Template.");
                    }else {

                        validate_POIdsOnShowPage(newlyCreatedServiceDataId,expectedPoList,customAssert);

                        validate_POIdsOnSDListPage(expectedPoList,customAssert);

                    }


                } else {
                    String errorMessage = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);
                    customAssert.assertTrue(false, "Bulk Create Scheduler Job Failed whereas it was supposed to Pass. Error Message: " + errorMessage);
                }
            } else {
                customAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + bulkCreateUploadResponse + "]");
            }

            FileUtils.deleteFile(bulkCreateExcelFilePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Verifying BulkCreate Flow [" + flowToTest + "]. " + e.getMessage());
        }

        customAssert.assertAll();
    }


    private HashMap<String,String> getPoIds(int serviceDataId,CustomAssert customAssert){

        HashMap<String,String> poIds = new HashMap<>();
        try{

            Show show = new Show();
            show.hitShowVersion2(sdEntityTypeId,serviceDataId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);

            JSONArray valuesArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("purchaseOrder").getJSONArray("values");

            for(int i = 0;i<valuesArray.length();i++){

                String name = valuesArray.getJSONObject(i).get("name").toString();
                String id = valuesArray.getJSONObject(i).get("id").toString();

                poIds.put(id,name);
            }

        }catch (Exception e){
            logger.error("Exception while getting Po Id from service Data");
            customAssert.assertTrue(false,"Exception while getting Po Id from service Data " + e.getStackTrace());
        }
        return poIds;
    }

    private void validate_POIdsOnShowPage(int serviceDataId,ArrayList<Integer> expectedPoList,CustomAssert customAssert){

        HashMap<String, String> poIdsONShowPage = getPoIds(serviceDataId, customAssert);
        if (poIdsONShowPage.size() == 0) {
            logger.error("On Service Data Show Page Number of purchase order ids is zero");
            customAssert.assertTrue(false, "On Service Data Show Page Number of purchase order ids is zero");
        } else {

            if(poIdsONShowPage.size() != expectedPoList.size()){
                customAssert.assertTrue(false,"Expected number of Po Ids on Show page not equal to actual number of PoIds Expected number : " + expectedPoList.size() + " Actual number : " + poIdsONShowPage.size());
            }else {

                for(Integer poId : expectedPoList) {

                    if(!poIdsONShowPage.containsKey(poId.toString())){
                        customAssert.assertTrue(false,"PO ID " + poId + " not present on purchase order show page");
                    }else {
                        String actualPoName= ShowHelper.getValueOfField(poEntityTypeId,poId,"name");
                        String poNameOnSDShowPage =  poIdsONShowPage.get(poId.toString());
                        if(!actualPoName.equals(poNameOnSDShowPage)){
                            logger.error("PO Name on Service Data not matched with actual PO Name Expected : " + actualPoName + " Actual : " + poNameOnSDShowPage);
                            customAssert.assertTrue(false,"PO Name on Service Data not matched with actual PO Name Expected : " + actualPoName + " Actual : " + poNameOnSDShowPage);
                        }
                    }
                }
            }
        }
    }

    public void validate_POIdsOnSDListPage(ArrayList<Integer> expectedPoIdList,CustomAssert customAssert){

        try{
            int sdListId = 352;
            int poId = expectedPoIdList.get(0);
            String purchaseOrderColumnId = "20349";
            String serviceDataColumnId = "14219";

            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                    "\"filterJson\":{\"253\":{\"multiselectValues\":" +
                    "{\"SELECTEDDATA\":[{\"id\":\"" + poId + "\",\"name\":\"PO \"}]}," +
                    "\"filterId\":253,\"filterName\":\"poNumber\"}}}," +
                    "\"selectedColumns\":[{\"columnId\":14483,\"columnQueryName\":\"bulkcheckbox\"}," +
                    "{\"columnId\":14219,\"columnQueryName\":\"id\"}," +
                    "{\"columnId\":14220,\"columnQueryName\":\"display_name\"}," +
                    "{\"columnId\":" + purchaseOrderColumnId + ",\"columnQueryName\":\"purchaseorders\"}]}";

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(sdListId,payload);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listDataResponse)){
                logger.error("Listing Response is invalid json when checking PO on SD Listing page");
                customAssert.assertTrue(false,"Listing Response is invalid json when checking PO on SD Listing page");
            }else {
                Map<Integer,Map<String, String>> listMap = listRendererListData.getListColumnIdValueMap(listDataResponse);

                if(listMap.size() == 0){
                    logger.error("While validating PO Id On SD List Page after Applying Purchase Order Filter Listing page has zero records");
                    customAssert.assertTrue(false,"While validating PO Id On SD List Page after Applying Purchase Order Filter Listing page has zero records");
                }else {
                    for (Map.Entry<Integer,Map<String, String>> listRowData : listMap.entrySet()){

                        Map<String, String> listColValues = listRowData.getValue();

                        String poValuesOnListingPage =  listColValues.get(purchaseOrderColumnId);

                        for(Integer poid : expectedPoIdList){
                            String actualPoName= ShowHelper.getValueOfField(poEntityTypeId,poId,"name");
                            if(!poValuesOnListingPage.contains(actualPoName)){
                                logger.error("Purchase order id " + poid + " not found on Purchase Order Column on Service Data Listing page for service data id " + listColValues.get(serviceDataColumnId));
                                customAssert.assertTrue(false,"Purchase order id " + poid + " not found on Purchase Order Column on Service Data Listing page for service data id " + listColValues.get(serviceDataColumnId));
                            }
                        }
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating PO Id On SD List Page " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating PO Id On SD List Page " + e.getStackTrace());
        }
    }

    private void updateUniqueFields(String filePath,String fileName,String sheetToUpdate,String[] uniqueFields){

        try {
            List<String> rowDataList = XLSUtils.getExcelDataOfOneRow(filePath, fileName, sheetToUpdate, 2);
            int rowToUpdate = 6;
            String updatedValue;

            for (int i = 0; i < uniqueFields.length; i++) {

                for (int j = 0; j < rowDataList.size(); j++) {

                    if (rowDataList.get(j).equalsIgnoreCase(uniqueFields[i])) {

                        String unqString = DateUtils.getCurrentTimeStamp();
                        unqString = unqString.replaceAll("_","");
                        unqString = unqString.replaceAll(" ","");

                        updatedValue = new BigDecimal(unqString).toString().substring(10);
                        List<CellType> cellTypes = XLSUtils.getAllCellTypeOfRow(filePath,fileName,sheetToUpdate,rowToUpdate);

                        XLSUtils.updateColumnValue(filePath, fileName, sheetToUpdate, rowToUpdate, j, updatedValue);


                    }
                }

            }
        }catch (Exception e){
            logger.error("Error while updating unique fields");
        }

    }

}
